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
import com.example.jpaspringboot.service.agent.AgentTaskService;
import com.example.jpaspringboot.service.agent.RedisAgentDispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        "hnblue.agent.redis.stream=hnblue:rc3:it:recovery",
        "hnblue.agent.redis.group=hnblue-rc3-recovery-workers",
        "hnblue.agent.redis.consumer=recovery-worker",
        "hnblue.agent.redis.publish-ms=3600000",
        "hnblue.agent.redis.consume-ms=3600000",
        "hnblue.agent.redis.retry-after=60s",
        "hnblue.agent.redis.max-attempts=3",
        "hnblue.agent.timeout-scan-ms=3600000"
})
@EnabledIfSystemProperty(named = "hnblue.durable.recovery.it", matches = "true")
@DirtiesContext
class RealDurableRecoveryIntegrationTest {
    private static final String STREAM = "hnblue:rc3:it:recovery";

    @Autowired ProfessionalAccountRepository professionals;
    @Autowired AgentEntitlementRepository entitlements;
    @Autowired AgentTaskRepository taskRepository;
    @Autowired AgentDispatchOutboxRepository outboxes;
    @Autowired AgentQuotaReservationRepository reservations;
    @Autowired AgentTaskOrchestrator orchestrator;
    @Autowired AgentTaskService tasks;
    @Autowired RedisAgentDispatchService dispatch;
    @Autowired StringRedisTemplate redis;

    @Test
    void requeuesAndRepublishesAStaleRunningTaskWithoutDoubleReservingQuota() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ProfessionalAccount professional = new ProfessionalAccount();
        professional.setUsername("rc3-recovery-" + suffix);
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
        String taskId = "rc3-recovery-" + suffix;
        AgentTask queued = orchestrator.submit(principal, AgentCapability.BASIC_QA,
                100, 120, "RC3 recovery probe", taskId);
        dispatch.publishAvailable();

        AgentDispatchOutbox firstPublish = outboxes.findByTaskId(taskId).orElseThrow();
        assertEquals(AgentDispatchOutbox.Status.PUBLISHED, firstPublish.getStatus());
        assertEquals(1, firstPublish.getAttempts());
        long streamSizeAfterFirstPublish = redis.opsForStream().size(STREAM);

        AgentTask running = tasks.markRunning(taskId);
        assertEquals(AgentTaskStatus.RUNNING, running.getStatus());
        firstPublish.setAvailableAt(LocalDateTime.now().minusSeconds(1));
        outboxes.saveAndFlush(firstPublish);

        dispatch.publishAvailable();

        AgentTask recovered = taskRepository.findById(taskId).orElseThrow();
        AgentDispatchOutbox republished = outboxes.findByTaskId(taskId).orElseThrow();
        assertEquals(AgentTaskStatus.QUEUED, recovered.getStatus());
        assertEquals(1, recovered.getAttempts());
        assertEquals(2, republished.getAttempts());
        assertTrue(redis.opsForStream().size(STREAM) > streamSizeAfterFirstPublish);
        assertEquals(AgentQuotaReservation.Status.RESERVED,
                reservations.findById(recovered.getReservationId()).orElseThrow().getStatus());

        orchestrator.cancel(taskId, principal);
        assertEquals(AgentQuotaReservation.Status.RELEASED,
                reservations.findById(recovered.getReservationId()).orElseThrow().getStatus());
    }
}
