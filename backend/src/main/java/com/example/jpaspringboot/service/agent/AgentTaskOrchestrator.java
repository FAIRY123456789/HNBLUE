package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.*;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.security.HnbluePrincipal;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class AgentTaskOrchestrator {
    private final AgentEntitlementService entitlements;
    private final AgentQuotaService quotas;
    private final AgentTaskService tasks;
    private final AgentGatewayClient gateway;
    private final Executor executor;
    private final Counter accepted;
    private final Counter rejected;
    private RedisAgentDispatchService redisDispatch;
    private AgentTaskExecutionService executionService;

    public AgentTaskOrchestrator(AgentEntitlementService entitlements, AgentQuotaService quotas,
                                 AgentTaskService tasks, AgentGatewayClient gateway,
                                 @Qualifier("agentTaskExecutor") Executor executor,
                                 MeterRegistry meterRegistry) {
        this.entitlements = entitlements;
        this.quotas = quotas;
        this.tasks = tasks;
        this.gateway = gateway;
        this.executor = executor;
        this.accepted = meterRegistry.counter("hnblue.agent.tasks.accepted");
        this.rejected = meterRegistry.counter("hnblue.agent.tasks.rejected");
    }

    @Autowired(required = false)
    void setRedisDispatch(RedisAgentDispatchService redisDispatch) {
        this.redisDispatch = redisDispatch;
    }

    @Autowired
    void setExecutionService(AgentTaskExecutionService executionService) {
        this.executionService = executionService;
    }

    public AgentTask submit(HnbluePrincipal principal, AgentCapability capability, int tokenBudget,
                            int timeoutSeconds, String message, String idempotencyKey) {
        String taskId = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString() : normalizeId(idempotencyKey);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            AgentTask existing = tasks.findIdempotentSubmission(taskId, principal, capability,
                    tokenBudget, message).orElse(null);
            if (existing != null) return existing;
        }
        String reservationId = "quota:" + taskId;
        quotas.reserve(principal, capability, tokenBudget, reservationId);
        int priority = entitlements.planFor(principal).queuePriority();
        String traceId = UUID.randomUUID().toString();
        LocalDateTime deadline = LocalDateTime.now().plusSeconds(Math.max(5, Math.min(timeoutSeconds, 600)));
        AgentTask task = redisDispatch == null
                ? tasks.create(taskId, traceId, principal, capability, reservationId, tokenBudget,
                        priority, deadline, message)
                : redisDispatch.createQueued(taskId, traceId, principal, capability, reservationId,
                        tokenBudget, priority, deadline, message);
        if (redisDispatch != null) {
            accepted.increment();
            return tasks.findAuthorized(taskId, principal);
        }
        try {
            executor.execute(() -> run(task, principal, message));
            accepted.increment();
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            rejected.increment();
            tasks.fail(taskId, "executor_overloaded", "Agent execution queue is full");
            quotas.release(reservationId);
        }
        return tasks.findAuthorized(taskId, principal);
    }

    public AgentTask cancel(String taskId, HnbluePrincipal principal) {
        AgentTask task = tasks.cancel(taskId, principal);
        if (task.getStatus() == AgentTaskStatus.CANCELLED) quotas.release(task.getReservationId());
        return task;
    }

    private void run(AgentTask submitted, HnbluePrincipal principal, String message) {
        if (executionService != null) {
            executionService.run(submitted.getTaskId());
            return;
        }
        AgentTask running = tasks.markRunning(submitted.getTaskId());
        if (running.getStatus() != AgentTaskStatus.RUNNING) return;
        try {
            AgentGatewayContract.Request request = new AgentGatewayContract.Request(
                    running.getTaskId(), running.getTraceId(), principal.subjectKey(), principal.role().name(),
                    running.getCapability(), allowedTools(running.getCapability()), running.getRequestedTokens(),
                    running.getCapability() == AgentCapability.MULTI_AGENT ? 8 : 4, message);
            AgentGatewayContract.Result result = gateway.execute(request);
            AgentTask completed = tasks.complete(running.getTaskId(), result);
            if (completed.getStatus() == AgentTaskStatus.SUCCEEDED
                    || completed.getStatus() == AgentTaskStatus.PARTIAL
                    || completed.getStatus() == AgentTaskStatus.ARCHIVED) {
                quotas.settle(running.getReservationId(), result.tokenUsage());
            } else if (completed.getStatus() != AgentTaskStatus.AWAITING_REVIEW
                    && completed.getStatus() != AgentTaskStatus.AWAITING_ARCHIVE) {
                // Cancellation, timeout/failure, or a rejected late result all release the reservation.
                quotas.release(running.getReservationId());
            }
        } catch (Exception ex) {
            tasks.fail(running.getTaskId(), "execution_failed", ex.getClass().getSimpleName());
            quotas.release(running.getReservationId());
        }
    }

    @Scheduled(fixedDelayString = "${hnblue.agent.timeout-scan-ms:60000}")
    public void expireTimedOutTasks() {
        for (AgentTask task : tasks.timedOut()) {
            tasks.fail(task.getTaskId(), "deadline_exceeded", "Agent task exceeded its deadline");
            quotas.release(task.getReservationId());
        }
    }

    private List<String> allowedTools(AgentCapability capability) {
        return switch (capability) {
            case BASIC_QA -> List.of("bluecarbon.knowledge.search");
            case PROFESSIONAL_ANALYSIS -> List.of("bluecarbon.knowledge.search", "bluecarbon.region.metrics.read", "bluecarbon.source.verify");
            case MULTI_AGENT -> List.of("bluecarbon.knowledge.search", "bluecarbon.region.metrics.read", "bluecarbon.methodology.lookup", "bluecarbon.source.verify");
        };
    }

    private String normalizeId(String value) {
        String normalized = value.trim();
        if (!normalized.matches("^[a-zA-Z0-9._:-]{8,64}$")) throw new IllegalArgumentException("invalid idempotency key");
        return normalized;
    }
}
