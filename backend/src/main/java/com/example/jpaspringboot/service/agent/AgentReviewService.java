package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.*;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.entity.AgentTaskReviewAction;
import com.example.jpaspringboot.repository.AgentTaskRepository;
import com.example.jpaspringboot.repository.AgentTaskReviewActionRepository;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.HnbluePrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class AgentReviewService {
    private final AgentTaskService tasks;
    private final AgentTaskRepository taskRepository;
    private final AgentTaskReviewActionRepository actions;
    private final AgentGatewayClient gateway;
    private final AgentQuotaService quotas;

    public AgentReviewService(AgentTaskService tasks, AgentTaskRepository taskRepository,
                              AgentTaskReviewActionRepository actions, AgentGatewayClient gateway,
                              AgentQuotaService quotas) {
        this.tasks = tasks;
        this.taskRepository = taskRepository;
        this.actions = actions;
        this.gateway = gateway;
        this.quotas = quotas;
    }

    @Transactional
    public AgentTask review(String taskId, String requestId, AgentReviewActionType action,
                            String comment, int reviewCycle, long expectedVersion,
                            HnbluePrincipal reviewer) {
        AgentTaskReviewAction existing = actions.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            if (!existing.getTaskId().equals(taskId)
                    || existing.getAction() != action
                    || !existing.getReviewerKey().equals(reviewer.subjectKey())) {
                throw new AgentReviewException("review_idempotency_conflict",
                        "review request id was used for a different decision");
            }
            return tasks.findForReview(taskId);
        }

        AgentTask task = tasks.findForReview(taskId);
        authorize(task, action, reviewer);
        if (task.getVersion() != expectedVersion) {
            throw new AgentReviewException("stale_task_version", "task changed after the review page was loaded");
        }
        if (task.getReviewCycle() != reviewCycle) {
            throw new AgentReviewException("stale_review_cycle", "review cycle is stale");
        }

        AgentTaskStatus before = task.getStatus();
        AgentGatewayContract.ReviewCommand command = new AgentGatewayContract.ReviewCommand(
                taskId, task.getTraceId(), requestId, action, reviewer.subjectKey(),
                reviewer.role().name(), comment == null ? "" : comment.trim(), reviewCycle);
        AgentGatewayContract.Result result;
        try {
            result = gateway.review(command);
        } catch (IOException ex) {
            throw new AgentReviewException("review_gateway_unavailable",
                    "review decision was not accepted by the Agent runtime", ex);
        }
        if (result.errorCode() != null && result.errorCode().startsWith("gateway_")) {
            throw new AgentReviewException(result.errorCode(),
                    "review decision was not accepted by the Agent runtime");
        }

        AgentTask updated = tasks.completeReview(taskId, result);
        AgentTaskReviewAction audit = new AgentTaskReviewAction();
        audit.setRequestId(requestId);
        audit.setTaskId(taskId);
        audit.setAction(action);
        audit.setReviewerKey(reviewer.subjectKey());
        audit.setReviewerRole(reviewer.role().name());
        audit.setReviewCycle(reviewCycle);
        audit.setFromStatus(before);
        audit.setToStatus(updated.getStatus());
        audit.setComment(comment == null ? "" : comment.trim());
        actions.save(audit);

        if (updated.getStatus() == AgentTaskStatus.ARCHIVED) {
            quotas.settle(updated.getReservationId(), result.tokenUsage());
        } else if (updated.getStatus() == AgentTaskStatus.REJECTED
                || updated.getStatus() == AgentTaskStatus.FAILED) {
            quotas.release(updated.getReservationId());
        }
        return updated;
    }

    public List<AgentTask> reviewQueue(HnbluePrincipal reviewer) {
        if (reviewer.role() == AccountRole.PUBLIC) {
            throw new AgentReviewException("review_role_forbidden", "public accounts cannot review Agent tasks");
        }
        List<AgentTaskStatus> statuses = reviewer.role() == AccountRole.ADMIN
                ? List.of(AgentTaskStatus.AWAITING_REVIEW, AgentTaskStatus.AWAITING_ARCHIVE)
                : List.of(AgentTaskStatus.AWAITING_REVIEW);
        return taskRepository.findTop100ByStatusInOrderByUpdatedAtAsc(statuses).stream()
                .filter(task -> !task.getOwnerKey().equals(reviewer.subjectKey()))
                .filter(task -> reviewer.role() == AccountRole.ADMIN
                        || !"SENIOR_REVIEWER".equals(task.getReviewLevel()))
                .toList();
    }

    public List<AgentTaskReviewAction> history(String taskId, HnbluePrincipal principal) {
        AgentTask task = tasks.findForReview(taskId);
        if (principal.role() == AccountRole.PUBLIC
                || (principal.role() != AccountRole.ADMIN
                    && !task.getOwnerKey().equals(principal.subjectKey()))) {
            throw new AgentReviewException("review_history_forbidden", "review history is not visible");
        }
        return actions.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    private void authorize(AgentTask task, AgentReviewActionType action, HnbluePrincipal reviewer) {
        if (task.getOwnerKey().equals(reviewer.subjectKey())) {
            throw new AgentReviewException("self_review_forbidden", "task owner cannot review their own result");
        }
        if (task.getStatus() == AgentTaskStatus.AWAITING_REVIEW) {
            if ("SENIOR_REVIEWER".equals(task.getReviewLevel())
                    && reviewer.role() != AccountRole.ADMIN) {
                throw new AgentReviewException("senior_review_forbidden",
                        "inconsistent evidence requires an administrator");
            }
            if (!Set.of(AccountRole.PROFESSIONAL, AccountRole.ADMIN).contains(reviewer.role())) {
                throw new AgentReviewException("review_role_forbidden", "domain review requires a professional account");
            }
            if (!Set.of(AgentReviewActionType.APPROVE, AgentReviewActionType.RETURN,
                    AgentReviewActionType.REJECT).contains(action)) {
                throw new AgentReviewException("review_action_forbidden", "action is invalid for domain review");
            }
            return;
        }
        if (task.getStatus() == AgentTaskStatus.AWAITING_ARCHIVE) {
            if (reviewer.role() != AccountRole.ADMIN) {
                throw new AgentReviewException("archive_role_forbidden", "archive review requires an administrator");
            }
            if (!Set.of(AgentReviewActionType.ARCHIVE, AgentReviewActionType.RETURN,
                    AgentReviewActionType.REJECT).contains(action)) {
                throw new AgentReviewException("review_action_forbidden", "action is invalid for archive review");
            }
            return;
        }
        throw new AgentReviewException("review_not_waiting", "task is not waiting for human review");
    }
}
