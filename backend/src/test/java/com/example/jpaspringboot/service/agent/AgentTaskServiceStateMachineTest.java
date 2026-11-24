package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentGatewayContract;
import com.example.jpaspringboot.agent.AgentExecutionStatus;
import com.example.jpaspringboot.agent.AgentTaskStatus;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.repository.AgentTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AgentTaskServiceStateMachineTest {
    private AgentTaskRepository repository;
    private AgentTaskService service;

    @BeforeEach
    void setUp() {
        repository = mock(AgentTaskRepository.class);
        service = new AgentTaskService(repository, new ObjectMapper());
    }

    @Test
    void lateSuccessCannotResurrectCancelledTask() {
        AgentTask task = task(AgentTaskStatus.CANCELLED);
        when(repository.findById("task-12345678")).thenReturn(Optional.of(task));

        AgentTask result = service.complete("task-12345678", success());

        assertEquals(AgentTaskStatus.CANCELLED, result.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void lateSuccessCannotResurrectDeadlineFailure() {
        AgentTask task = task(AgentTaskStatus.FAILED);
        task.setErrorCode("deadline_exceeded");
        when(repository.findById("task-12345678")).thenReturn(Optional.of(task));

        AgentTask result = service.complete("task-12345678", success());

        assertEquals(AgentTaskStatus.FAILED, result.getStatus());
        assertEquals("deadline_exceeded", result.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void duplicateWorkerCannotRestartTerminalTask() {
        AgentTask task = task(AgentTaskStatus.SUCCEEDED);
        task.setAttempts(1);
        when(repository.findById("task-12345678")).thenReturn(Optional.of(task));

        AgentTask result = service.markRunning("task-12345678");

        assertEquals(AgentTaskStatus.SUCCEEDED, result.getStatus());
        assertEquals(1, result.getAttempts());
        verify(repository, never()).save(any());
    }

    @Test
    void terminalSuccessCannotBeOverwrittenByFailure() {
        AgentTask task = task(AgentTaskStatus.SUCCEEDED);
        when(repository.findById("task-12345678")).thenReturn(Optional.of(task));

        AgentTask result = service.fail("task-12345678", "late_failure", "too late");

        assertEquals(AgentTaskStatus.SUCCEEDED, result.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void runningTaskCanCompleteExactlyOnce() {
        AgentTask task = task(AgentTaskStatus.RUNNING);
        when(repository.findById("task-12345678")).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);

        AgentTask result = service.complete("task-12345678", success());

        assertEquals(AgentTaskStatus.SUCCEEDED, result.getStatus());
        assertEquals(7, result.getActualTokens());
        assertTrue(result.getResultJson().contains("hnblue-route-v2-lora-s3407-merged"));
        assertTrue(result.getResultJson().contains("JAVA_POLICY_AND_EXISTING_RULES"));
        verify(repository).save(task);
    }

    @Test
    void runningTaskCanPauseForDomainReview() {
        AgentTask task = task(AgentTaskStatus.RUNNING);
        when(repository.findById("task-12345678")).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);

        AgentTask result = service.complete("task-12345678", reviewResult(
                AgentExecutionStatus.AWAITING_REVIEW, "DOMAIN_REVIEWER", 0));

        assertEquals(AgentTaskStatus.AWAITING_REVIEW, result.getStatus());
        assertEquals("DOMAIN_REVIEWER", result.getReviewLevel());
        assertEquals(0, result.getReviewCycle());
        verify(repository).save(task);
    }

    @Test
    void domainApprovalCanPauseForArchiveReview() {
        AgentTask task = task(AgentTaskStatus.AWAITING_REVIEW);
        when(repository.findById("task-12345678")).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);

        AgentTask result = service.completeReview("task-12345678", reviewResult(
                AgentExecutionStatus.AWAITING_ARCHIVE, "ARCHIVE_OFFICER", 0));

        assertEquals(AgentTaskStatus.AWAITING_ARCHIVE, result.getStatus());
        assertEquals("ARCHIVE_OFFICER", result.getReviewLevel());
        verify(repository).save(task);
    }

    @Test
    void archiveDecisionIsTerminal() {
        AgentTask task = task(AgentTaskStatus.AWAITING_ARCHIVE);
        task.setReviewLevel("ARCHIVE_OFFICER");
        when(repository.findById("task-12345678")).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);

        AgentTask result = service.completeReview("task-12345678", reviewResult(
                AgentExecutionStatus.ARCHIVED, null, 0));

        assertEquals(AgentTaskStatus.ARCHIVED, result.getStatus());
        assertEquals(null, result.getReviewLevel());
        verify(repository).save(task);
    }

    private AgentTask task(AgentTaskStatus status) {
        AgentTask task = new AgentTask();
        task.setTaskId("task-12345678");
        task.setStatus(status);
        return task;
    }

    private AgentGatewayContract.Result success() {
        AgentGatewayContract.RoutingAudit audit = new AgentGatewayContract.RoutingAudit(
                "SHADOW", "JAVA_POLICY_AND_EXISTING_RULES", "rule-r0", "education",
                "local-route-v2", true, "education", true, "ROUTED", "EDUCATION",
                "EDUCATION_CONTENT", "ALLOW", false, java.util.List.of(), 0.91,
                "hnblue-route-v2-lora-s3407-merged", 12.5, null);
        return new AgentGatewayContract.Result(true, false, "ok", java.util.List.of(),
                7, null, null, audit, com.example.jpaspringboot.agent.AgentExecutionStatus.COMPLETED,
                null, java.util.Map.of());
    }

    private AgentGatewayContract.Result reviewResult(AgentExecutionStatus status,
                                                      String level, int cycle) {
        AgentGatewayContract.ReviewRequest request = level == null ? null
                : new AgentGatewayContract.ReviewRequest(level, cycle, "task-12345678",
                "trace-12345678", "draft", "VERIFIED", 2,
                java.util.List.of(), java.util.List.of("APPROVE", "RETURN", "REJECT"));
        return new AgentGatewayContract.Result(true, false, "draft", java.util.List.of(),
                9, null, null, null, status, request, java.util.Map.of("agentMessages", 4));
    }
}
