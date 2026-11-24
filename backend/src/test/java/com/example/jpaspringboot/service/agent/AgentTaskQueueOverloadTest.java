package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.AgentGatewayContract;
import com.example.jpaspringboot.agent.AgentTaskStatus;
import com.example.jpaspringboot.agent.EntitlementPlan;
import com.example.jpaspringboot.config.AgentExecutionConfig;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.HnbluePrincipal;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentTaskQueueOverloadTest {
    private AgentEntitlementService entitlements;
    private AgentQuotaService quotas;
    private AgentTaskService tasks;
    private AgentGatewayClient gateway;
    private ThreadPoolTaskExecutor executor;
    private AgentTaskOrchestrator orchestrator;
    private final Map<String, AgentTask> taskState = new ConcurrentHashMap<>();
    private final CountDownLatch gatewayEntered = new CountDownLatch(1);
    private final CountDownLatch releaseGateway = new CountDownLatch(1);

    @BeforeEach
    void setUp() throws Exception {
        entitlements = mock(AgentEntitlementService.class);
        quotas = mock(AgentQuotaService.class);
        tasks = mock(AgentTaskService.class);
        gateway = mock(AgentGatewayClient.class);
        executor = (ThreadPoolTaskExecutor) new AgentExecutionConfig().agentTaskExecutor(1, 1, 1);
        orchestrator = new AgentTaskOrchestrator(entitlements, quotas, tasks, gateway, executor,
                new SimpleMeterRegistry());

        when(entitlements.planFor(any())).thenReturn(new EntitlementPlan(
                Set.of(AgentCapability.BASIC_QA), 100, 100_000, 2_000, 20, 0));
        when(tasks.create(anyString(), anyString(), any(), any(), anyString(), anyInt(), anyInt(),
                any(LocalDateTime.class), anyString()))
                .thenAnswer(invocation -> {
                    AgentTask task = new AgentTask();
                    task.setTaskId(invocation.getArgument(0));
                    task.setTraceId(invocation.getArgument(1));
                    task.setCapability(invocation.getArgument(3));
                    task.setReservationId(invocation.getArgument(4));
                    task.setRequestedTokens(invocation.getArgument(5));
                    task.setPriority(invocation.getArgument(6));
                    task.setDeadline(invocation.getArgument(7));
                    task.setStatus(AgentTaskStatus.QUEUED);
                    taskState.put(task.getTaskId(), task);
                    return task;
                });
        when(tasks.findAuthorized(anyString(), any())).thenAnswer(invocation -> taskState.get(invocation.getArgument(0)));
        when(tasks.markRunning(anyString())).thenAnswer(invocation -> {
            AgentTask task = taskState.get(invocation.getArgument(0));
            task.setStatus(AgentTaskStatus.RUNNING);
            return task;
        });
        when(gateway.execute(any())).thenAnswer(invocation -> {
            gatewayEntered.countDown();
            releaseGateway.await(5, TimeUnit.SECONDS);
            return AgentGatewayContract.Result.failed("test_release", "released by test");
        });
        when(tasks.complete(anyString(), any())).thenAnswer(invocation -> {
            AgentTask task = taskState.get(invocation.getArgument(0));
            task.setStatus(AgentTaskStatus.FAILED);
            return task;
        });
    }

    @AfterEach
    void tearDown() {
        releaseGateway.countDown();
        executor.shutdown();
    }

    @Test
    void fullBoundedQueueFailsThirdTaskAndReleasesItsQuota() throws Exception {
        HnbluePrincipal principal = new HnbluePrincipal(7L, "professional-7", AccountRole.PROFESSIONAL, 0, false);

        orchestrator.submit(principal, AgentCapability.BASIC_QA, 100, 30, "one", "queue-one");
        assertTrue(gatewayEntered.await(2, TimeUnit.SECONDS), "first task must occupy the only worker");
        orchestrator.submit(principal, AgentCapability.BASIC_QA, 100, 30, "two", "queue-two");
        orchestrator.submit(principal, AgentCapability.BASIC_QA, 100, 30, "three", "queue-three");

        verify(tasks, timeout(1_000)).fail("queue-three", "executor_overloaded", "Agent execution queue is full");
        verify(quotas, timeout(1_000)).release("quota:queue-three");
    }
}
