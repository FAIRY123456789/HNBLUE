package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.entity.AgentDispatchOutbox;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.repository.AgentDispatchOutboxRepository;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.HnbluePrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisAgentDispatchPayloadTest {
    @Test
    void outboxContainsOnlyVersionedTransportMetadata() {
        AgentTaskService tasks = mock(AgentTaskService.class);
        AgentTaskExecutionService execution = mock(AgentTaskExecutionService.class);
        AgentQuotaService quotas = mock(AgentQuotaService.class);
        AgentDispatchOutboxRepository outboxes = mock(AgentDispatchOutboxRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisAgentDispatchService service = new RedisAgentDispatchService(tasks, execution, quotas,
                outboxes, redis, "test-stream", "test-group", "test-consumer",
                Duration.ofSeconds(10), 3);
        HnbluePrincipal owner = new HnbluePrincipal(
                9L, "sensitive-user", AccountRole.PROFESSIONAL, 7, false);
        AgentTask task = new AgentTask();
        task.setTaskId("task-payload-01");
        when(tasks.create(eq("task-payload-01"), anyString(), eq(owner),
                eq(AgentCapability.PROFESSIONAL_ANALYSIS), eq("quota:task-payload-01"),
                eq(1000), eq(10), any(LocalDateTime.class), eq("sensitive question")))
                .thenReturn(task);

        service.createQueued("task-payload-01", "trace-01", owner,
                AgentCapability.PROFESSIONAL_ANALYSIS, "quota:task-payload-01", 1000,
                10, LocalDateTime.now().plusMinutes(1), "sensitive question");

        ArgumentCaptor<AgentDispatchOutbox> captor = ArgumentCaptor.forClass(AgentDispatchOutbox.class);
        verify(outboxes).save(captor.capture());
        String payload = captor.getValue().getPayloadJson();
        assertEquals("{\"schemaVersion\":1}", payload);
        assertFalse(payload.contains("sensitive-user"));
        assertFalse(payload.contains("sensitive question"));
    }
}
