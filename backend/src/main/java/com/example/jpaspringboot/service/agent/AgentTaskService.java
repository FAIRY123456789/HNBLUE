package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.*;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.repository.AgentTaskRepository;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.HnbluePrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AgentTaskService {
    private final AgentTaskRepository tasks;
    private final ObjectMapper mapper;

    public AgentTaskService(AgentTaskRepository tasks, ObjectMapper mapper) {
        this.tasks = tasks;
        this.mapper = mapper;
    }

    @Transactional
    public AgentTask create(String taskId, String traceId, HnbluePrincipal owner, AgentCapability capability,
                            String reservationId, int tokens, int priority, LocalDateTime deadline,
                            String message) {
        AgentTask task = new AgentTask();
        task.setTaskId(taskId);
        task.setTraceId(traceId);
        task.setOwnerKey(owner.subjectKey());
        task.setOwnerRole(owner.role().name());
        task.setOwnerUsername(owner.username());
        task.setOwnerTokenVersion(owner.tokenVersion());
        task.setPasswordChangeRequired(owner.passwordChangeRequired());
        task.setInputMessage(message);
        task.setCapability(capability);
        task.setStatus(AgentTaskStatus.QUEUED);
        task.setReservationId(reservationId);
        task.setRequestedTokens(tokens);
        task.setPriority(priority);
        task.setDeadline(deadline);
        return tasks.save(task);
    }

    public Optional<AgentTask> findIdempotentSubmission(String taskId, HnbluePrincipal principal,
                                                         AgentCapability capability, int tokens,
                                                         String message) {
        Optional<AgentTask> found = tasks.findById(taskId);
        if (found.isEmpty()) return Optional.empty();
        AgentTask task = found.get();
        if (!task.getOwnerKey().equals(principal.subjectKey())) {
            throw new IdempotencyConflictException("idempotency key belongs to another account");
        }
        if (task.getCapability() != capability || task.getRequestedTokens() != tokens
                || !java.util.Objects.equals(task.getInputMessage(), message)) {
            throw new IdempotencyConflictException("idempotency key was already used for a different request");
        }
        return Optional.of(task);
    }

    public AgentTask findForExecution(String taskId) {
        return require(taskId);
    }

    public AgentTask findForReview(String taskId) {
        return require(taskId);
    }

    @Transactional
    public AgentTask markRunning(String taskId) {
        AgentTask task = require(taskId);
        if (task.getStatus() != AgentTaskStatus.QUEUED) return task;
        task.setStatus(AgentTaskStatus.RUNNING);
        task.setAttempts(task.getAttempts() + 1);
        return tasks.save(task);
    }

    @Transactional
    public AgentTask complete(String taskId, AgentGatewayContract.Result result) {
        AgentTask task = require(taskId);
        // A response can arrive after cancellation/deadline processing. Never resurrect a terminal task.
        if (task.getStatus() != AgentTaskStatus.RUNNING) return task;
        return applyGatewayResult(task, result);
    }

    @Transactional
    public AgentTask completeReview(String taskId, AgentGatewayContract.Result result) {
        AgentTask task = require(taskId);
        if (!List.of(AgentTaskStatus.AWAITING_REVIEW, AgentTaskStatus.AWAITING_ARCHIVE)
                .contains(task.getStatus())) return task;
        return applyGatewayResult(task, result);
    }

    private AgentTask applyGatewayResult(AgentTask task, AgentGatewayContract.Result result) {
        task.setActualTokens(Math.max(0, result.tokenUsage()));
        AgentExecutionStatus execution = result.executionStatus() == null
                ? AgentExecutionStatus.COMPLETED : result.executionStatus();
        if (execution == AgentExecutionStatus.AWAITING_REVIEW
                || execution == AgentExecutionStatus.AWAITING_ARCHIVE) {
            task.setStatus(execution == AgentExecutionStatus.AWAITING_REVIEW
                    ? AgentTaskStatus.AWAITING_REVIEW : AgentTaskStatus.AWAITING_ARCHIVE);
            task.setResultJson(writeResult(result));
            if (result.reviewRequest() != null) {
                task.setReviewLevel(result.reviewRequest().level());
                task.setReviewCycle(result.reviewRequest().cycle());
            }
            task.setErrorCode(null);
            task.setErrorMessage(null);
        } else if (execution == AgentExecutionStatus.ARCHIVED) {
            task.setStatus(AgentTaskStatus.ARCHIVED);
            task.setResultJson(writeResult(result));
            task.setReviewLevel(null);
        } else if (execution == AgentExecutionStatus.REJECTED) {
            task.setStatus(AgentTaskStatus.REJECTED);
            task.setResultJson(writeResult(result));
            task.setErrorCode(safe(result.errorCode(), 80));
            task.setErrorMessage(safe(result.errorMessage(), 600));
            task.setReviewLevel(null);
        } else if (result.success()) {
            task.setStatus(result.partial() ? AgentTaskStatus.PARTIAL : AgentTaskStatus.SUCCEEDED);
            task.setResultJson(writeResult(result));
        } else {
            task.setStatus(AgentTaskStatus.FAILED);
            task.setErrorCode(safe(result.errorCode(), 80));
            task.setErrorMessage(safe(result.errorMessage(), 600));
        }
        return tasks.save(task);
    }

    @Transactional
    public AgentTask fail(String taskId, String code, String message) {
        AgentTask task = require(taskId);
        if (isTerminal(task.getStatus())) return task;
        task.setStatus(AgentTaskStatus.FAILED);
        task.setErrorCode(safe(code, 80));
        task.setErrorMessage(safe(message, 600));
        return tasks.save(task);
    }

    @Transactional
    public AgentTask cancel(String taskId, HnbluePrincipal principal) {
        AgentTask task = findAuthorized(taskId, principal);
        if (isTerminal(task.getStatus())) return task;
        task.setStatus(AgentTaskStatus.CANCELLED);
        return tasks.save(task);
    }

    public AgentTask findAuthorized(String taskId, HnbluePrincipal principal) {
        AgentTask task = require(taskId);
        if (principal.role() != AccountRole.ADMIN && !task.getOwnerKey().equals(principal.subjectKey())) {
            throw new SecurityException("task does not belong to current principal");
        }
        return task;
    }

    public List<AgentTask> timedOut() {
        return tasks.findTop100ByStatusInAndDeadlineBeforeOrderByDeadlineAsc(
                List.of(AgentTaskStatus.QUEUED, AgentTaskStatus.RUNNING), LocalDateTime.now());
    }

    @Transactional
    public AgentTask prepareRetry(String taskId, int maxAttempts) {
        AgentTask task = require(taskId);
        if (isTerminal(task.getStatus())) return task;
        if (task.getAttempts() >= maxAttempts) {
            task.setStatus(AgentTaskStatus.FAILED);
            task.setErrorCode("dispatch_retries_exhausted");
            task.setErrorMessage("Agent task exceeded the durable dispatch retry limit");
            return tasks.save(task);
        }
        if (task.getStatus() == AgentTaskStatus.RUNNING) {
            task.setStatus(AgentTaskStatus.QUEUED);
            return tasks.save(task);
        }
        return task;
    }

    private AgentTask require(String taskId) {
        return tasks.findById(taskId).orElseThrow(() -> new IllegalArgumentException("agent task not found"));
    }

    private boolean isTerminal(AgentTaskStatus status) {
        return List.of(AgentTaskStatus.SUCCEEDED, AgentTaskStatus.PARTIAL,
                AgentTaskStatus.REJECTED, AgentTaskStatus.ARCHIVED,
                AgentTaskStatus.FAILED, AgentTaskStatus.CANCELLED).contains(status);
    }

    private String writeResult(AgentGatewayContract.Result result) {
        try { return mapper.writeValueAsString(result); }
        catch (JsonProcessingException e) { throw new IllegalStateException("could not serialize Agent result", e); }
    }

    private String safe(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
