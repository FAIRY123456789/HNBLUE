package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentExecutionStatus;
import com.example.jpaspringboot.agent.AgentGatewayContract;
import com.example.jpaspringboot.agent.AgentReviewActionType;
import com.example.jpaspringboot.agent.AgentTaskStatus;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.entity.AgentTaskReviewAction;
import com.example.jpaspringboot.repository.AgentTaskRepository;
import com.example.jpaspringboot.repository.AgentTaskReviewActionRepository;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.HnbluePrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentReviewServiceTest {
    private AgentTaskService tasks;
    private AgentTaskRepository taskRepository;
    private AgentTaskReviewActionRepository actions;
    private AgentGatewayClient gateway;
    private AgentQuotaService quotas;
    private AgentReviewService service;

    @BeforeEach
    void setUp() {
        tasks = mock(AgentTaskService.class);
        taskRepository = mock(AgentTaskRepository.class);
        actions = mock(AgentTaskReviewActionRepository.class);
        gateway = mock(AgentGatewayClient.class);
        quotas = mock(AgentQuotaService.class);
        service = new AgentReviewService(tasks, taskRepository, actions, gateway, quotas);
    }

    @Test
    void ownerCannotReviewOwnTask() {
        AgentTask task = waitingTask(AgentTaskStatus.AWAITING_REVIEW, "PROFESSIONAL:11");
        when(tasks.findForReview(task.getTaskId())).thenReturn(task);

        AgentReviewException error = assertThrows(AgentReviewException.class, () -> service.review(
                task.getTaskId(), "review-request-0001", AgentReviewActionType.APPROVE,
                "looks good", 0, 0, principal(11L, AccountRole.PROFESSIONAL)));

        assertEquals("self_review_forbidden", error.getCode());
        verifyNoInteractions(gateway);
    }

    @Test
    void professionalCannotArchive() {
        AgentTask task = waitingTask(AgentTaskStatus.AWAITING_ARCHIVE, "PROFESSIONAL:10");
        when(tasks.findForReview(task.getTaskId())).thenReturn(task);

        AgentReviewException error = assertThrows(AgentReviewException.class, () -> service.review(
                task.getTaskId(), "review-request-0002", AgentReviewActionType.ARCHIVE,
                "archive", 0, 0, principal(12L, AccountRole.PROFESSIONAL)));

        assertEquals("archive_role_forbidden", error.getCode());
        verifyNoInteractions(gateway);
    }

    @Test
    void inconsistentEvidenceEscalatesToAdministrator() {
        AgentTask task = waitingTask(AgentTaskStatus.AWAITING_REVIEW, "PROFESSIONAL:10");
        task.setReviewLevel("SENIOR_REVIEWER");
        when(tasks.findForReview(task.getTaskId())).thenReturn(task);

        AgentReviewException error = assertThrows(AgentReviewException.class, () -> service.review(
                task.getTaskId(), "review-request-0005", AgentReviewActionType.APPROVE,
                "domain approval is not enough", 0, 0,
                principal(12L, AccountRole.PROFESSIONAL)));

        assertEquals("senior_review_forbidden", error.getCode());
        verifyNoInteractions(gateway);
    }

    @Test
    void repeatedRequestIdReturnsPersistedResultWithoutCallingRuntime() {
        AgentTaskReviewAction existing = new AgentTaskReviewAction();
        existing.setRequestId("review-request-0003");
        existing.setTaskId("task-12345678");
        existing.setAction(AgentReviewActionType.APPROVE);
        existing.setReviewerKey("PROFESSIONAL:12");
        AgentTask current = waitingTask(AgentTaskStatus.AWAITING_ARCHIVE, "PROFESSIONAL:10");
        when(actions.findByRequestId(existing.getRequestId())).thenReturn(Optional.of(existing));
        when(tasks.findForReview(existing.getTaskId())).thenReturn(current);

        AgentTask result = service.review(existing.getTaskId(), existing.getRequestId(),
                AgentReviewActionType.APPROVE, "same retry", 0, 0,
                principal(12L, AccountRole.PROFESSIONAL));

        assertEquals(AgentTaskStatus.AWAITING_ARCHIVE, result.getStatus());
        verifyNoInteractions(gateway);
        verify(actions, never()).save(any());
    }

    @Test
    void domainApprovalAdvancesCheckpointAndWritesAudit() throws IOException {
        AgentTask task = waitingTask(AgentTaskStatus.AWAITING_REVIEW, "PROFESSIONAL:10");
        AgentTask updated = waitingTask(AgentTaskStatus.AWAITING_ARCHIVE, "PROFESSIONAL:10");
        when(tasks.findForReview(task.getTaskId())).thenReturn(task);
        AgentGatewayContract.Result runtimeResult = awaitingArchive();
        when(gateway.review(any())).thenReturn(runtimeResult);
        when(tasks.completeReview(task.getTaskId(), runtimeResult)).thenReturn(updated);

        AgentTask result = service.review(task.getTaskId(), "review-request-0004",
                AgentReviewActionType.APPROVE, "evidence checked", 0, 0,
                principal(12L, AccountRole.PROFESSIONAL));

        assertEquals(AgentTaskStatus.AWAITING_ARCHIVE, result.getStatus());
        verify(gateway).review(argThat(command -> command.action() == AgentReviewActionType.APPROVE
                && command.reviewerSubjectKey().equals("PROFESSIONAL:12")));
        verify(actions).save(argThat(audit -> audit.getFromStatus() == AgentTaskStatus.AWAITING_REVIEW
                && audit.getToStatus() == AgentTaskStatus.AWAITING_ARCHIVE));
        verifyNoInteractions(quotas);
    }

    private AgentTask waitingTask(AgentTaskStatus status, String owner) {
        AgentTask task = new AgentTask();
        task.setTaskId("task-12345678");
        task.setTraceId("trace-12345678");
        task.setOwnerKey(owner);
        task.setStatus(status);
        task.setReviewCycle(0);
        task.setReservationId("reservation-12345678");
        return task;
    }

    private HnbluePrincipal principal(long id, AccountRole role) {
        return new HnbluePrincipal(id, "reviewer" + id, role, 0, false);
    }

    private AgentGatewayContract.Result awaitingArchive() {
        AgentGatewayContract.ReviewRequest review = new AgentGatewayContract.ReviewRequest(
                "ARCHIVE_OFFICER", 0, "task-12345678", "trace-12345678", "draft",
                "VERIFIED", 2, List.of(), List.of("ARCHIVE", "RETURN", "REJECT"));
        return new AgentGatewayContract.Result(true, false, "draft", List.of(), 12,
                null, null, null, AgentExecutionStatus.AWAITING_ARCHIVE, review, Map.of());
    }
}
