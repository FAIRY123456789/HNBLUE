package com.example.jpaspringboot.agent;

import com.example.jpaspringboot.entity.AgentDispatchOutbox;
import com.example.jpaspringboot.entity.AgentEntitlement;
import com.example.jpaspringboot.entity.AgentQuotaReservation;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.entity.ProfessionalAccount;
import com.example.jpaspringboot.repository.AgentDispatchOutboxRepository;
import com.example.jpaspringboot.repository.AgentEntitlementRepository;
import com.example.jpaspringboot.repository.AgentQuotaReservationRepository;
import com.example.jpaspringboot.repository.AgentTaskRepository;
import com.example.jpaspringboot.repository.ProfessionalAccountRepository;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.HnbluePrincipal;
import com.example.jpaspringboot.service.agent.AgentTaskOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver",
        "spring.jpa.open-in-view=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.defer-datasource-initialization=false",
        "spring.sql.init.mode=never",
        "spring.data.redis.repositories.enabled=false",
        "spring.data.redis.timeout=3s",
        "management.health.redis.enabled=false",
        "hnblue.agent.dispatch.mode=redis-stream",
        "hnblue.agent.gateway.enabled=false",
        "hnblue.agent.redis.stream=hnblue:rc3:it:durable",
        "hnblue.agent.redis.group=hnblue-rc3-it-workers",
        "hnblue.agent.redis.consumer=integration-worker",
        "hnblue.agent.redis.publish-ms=100",
        "hnblue.agent.redis.consume-ms=100",
        "hnblue.agent.redis.retry-after=10s",
        "hnblue.agent.timeout-scan-ms=60000"
})
@EnabledIfSystemProperty(named = "hnblue.durable.it", matches = "true")
@DirtiesContext
class RealDurableDispatchIntegrationTest {
    private static final String STREAM = "hnblue:rc3:it:durable";

    @Autowired ProfessionalAccountRepository professionals;
    @Autowired AgentEntitlementRepository entitlements;
    @Autowired AgentTaskRepository tasks;
    @Autowired AgentDispatchOutboxRepository outboxes;
    @Autowired AgentQuotaReservationRepository reservations;
    @Autowired AgentTaskOrchestrator orchestrator;
    @Autowired StringRedisTemplate redis;

    @Test
    void commitsOutboxPublishesConsumesAndSettlesFailureExactlyOnce() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ProfessionalAccount professional = new ProfessionalAccount();
        professional.setUsername("rc3-durable-" + suffix);
        professional.setPasswordHash("{noop}local-test-only");
        professional.setEnabled(true);
        professional.setPasswordChangeRequired(false);
        professional.setCreatedBy("RC3_TEST");
        professional = professionals.saveAndFlush(professional);

        AgentEntitlement entitlement = new AgentEntitlement();
        entitlement.setProfessionalAccountId(professional.getId());
        entitlement.setDailyRequestLimit(10);
        entitlement.setDailyTokenLimit(10_000);
        entitlement.setMaxTokensPerRequest(1_000);
        entitlements.saveAndFlush(entitlement);

        HnbluePrincipal principal = new HnbluePrincipal(professional.getId(), professional.getUsername(),
                AccountRole.PROFESSIONAL, professional.getTokenVersion(), false);
        String taskId = "rc3-durable-" + suffix;
        AgentTask accepted = orchestrator.submit(principal, AgentCapability.BASIC_QA,
                100, 30, "RC3 durable dispatch probe", taskId);
        assertEquals(AgentTaskStatus.QUEUED, accepted.getStatus());
        AgentTask duplicate = orchestrator.submit(principal, AgentCapability.BASIC_QA,
                100, 30, "RC3 durable dispatch probe", taskId);
        assertEquals(taskId, duplicate.getTaskId());
        assertThrows(com.example.jpaspringboot.service.agent.IdempotencyConflictException.class,
                () -> orchestrator.submit(principal, AgentCapability.BASIC_QA,
                        100, 30, "different request", taskId));
        assertTrue(outboxes.findByTaskId(taskId).isPresent());
        assertTrue(reservations.findById(accepted.getReservationId()).isPresent());

        AgentTask terminal = awaitTerminal(taskId, Duration.ofSeconds(15));
        assertEquals(AgentTaskStatus.FAILED, terminal.getStatus());
        assertEquals("gateway_disabled", terminal.getErrorCode());
        assertEquals(1, terminal.getAttempts());
        assertEquals("RC3 durable dispatch probe", terminal.getInputMessage());

        AgentDispatchOutbox outbox = outboxes.findByTaskId(taskId).orElseThrow();
        assertEquals(AgentDispatchOutbox.Status.CONSUMED, outbox.getStatus());
        assertEquals(1, outbox.getAttempts());
        assertNotNull(outbox.getPublishedAt());
        assertNotNull(outbox.getConsumedAt());
        assertEquals("{\"schemaVersion\":1}", outbox.getPayloadJson());

        AgentQuotaReservation reservation = reservations.findById(terminal.getReservationId()).orElseThrow();
        assertEquals(AgentQuotaReservation.Status.RELEASED, reservation.getStatus());
        assertTrue(redis.opsForStream().size(STREAM) >= 1);
        assertEquals(0L, redis.opsForStream().pending(STREAM, "hnblue-rc3-it-workers")
                .getTotalPendingMessages());
    }

    private AgentTask awaitTerminal(String taskId, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            AgentTask task = tasks.findById(taskId).orElseThrow();
            if (task.getStatus() == AgentTaskStatus.SUCCEEDED
                    || task.getStatus() == AgentTaskStatus.PARTIAL
                    || task.getStatus() == AgentTaskStatus.FAILED
                    || task.getStatus() == AgentTaskStatus.CANCELLED) {
                return task;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("durable task did not finish within " + timeout);
    }
}
