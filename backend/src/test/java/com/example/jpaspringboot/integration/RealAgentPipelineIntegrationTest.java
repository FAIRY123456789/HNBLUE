package com.example.jpaspringboot.integration;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.AgentGatewayContract;
import com.example.jpaspringboot.agent.AgentTaskStatus;
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
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in, no-mock acceptance of the complete durable path:
 * MySQL transaction/outbox -> Redis Stream -> Java gateway -> Python LangGraph
 * -> Ollama -> MySQL terminal state and quota settlement.
 */
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
        "hnblue.agent.gateway.enabled=true",
        "hnblue.agent.gateway.url=http://127.0.0.1:8890",
        "hnblue.agent.gateway.internal-token=stage3-integration-secret",
        "hnblue.agent.gateway.timeout=45s",
        "resilience4j.retry.instances.agentService.max-attempts=1",
        "hnblue.agent.redis.stream=hnblue:rc2:it:real-pipeline",
        "hnblue.agent.redis.group=hnblue-rc2-real-pipeline-workers",
        "hnblue.agent.redis.consumer=real-pipeline-worker",
        "hnblue.agent.redis.publish-ms=100",
        "hnblue.agent.redis.consume-ms=100",
        "hnblue.agent.redis.retry-after=60s",
        "hnblue.agent.timeout-scan-ms=60000"
})
@EnabledIfSystemProperty(named = "hnblue.real.pipeline.it", matches = "true")
@DirtiesContext
class RealAgentPipelineIntegrationTest {
    private static final String STREAM = "hnblue:rc2:it:real-pipeline";
    private static final String GROUP = "hnblue-rc2-real-pipeline-workers";

    @Autowired ProfessionalAccountRepository professionals;
    @Autowired AgentEntitlementRepository entitlements;
    @Autowired AgentTaskRepository tasks;
    @Autowired AgentDispatchOutboxRepository outboxes;
    @Autowired AgentQuotaReservationRepository reservations;
    @Autowired AgentTaskOrchestrator orchestrator;
    @Autowired StringRedisTemplate redis;
    @Autowired ObjectMapper mapper;

    @Test
    void executesRealOllamaTaskThroughDurableInfrastructure() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ProfessionalAccount professional = new ProfessionalAccount();
        professional.setUsername("rc2-pipeline-" + suffix);
        professional.setPasswordHash("{noop}local-test-only");
        professional.setEnabled(true);
        professional.setPasswordChangeRequired(false);
        professional.setCreatedBy("RC2_REAL_PIPELINE_TEST");
        professional = professionals.saveAndFlush(professional);

        AgentEntitlement entitlement = new AgentEntitlement();
        entitlement.setProfessionalAccountId(professional.getId());
        entitlement.setBasicQaEnabled(true);
        entitlement.setProfessionalAnalysisEnabled(true);
        entitlement.setMultiAgentEnabled(true);
        entitlement.setDailyRequestLimit(10);
        entitlement.setDailyTokenLimit(20_000);
        entitlement.setMaxTokensPerRequest(4_000);
        entitlement.setMaxConcurrentTasks(2);
        entitlement.setQueuePriority(20);
        entitlements.saveAndFlush(entitlement);

        HnbluePrincipal principal = new HnbluePrincipal(professional.getId(), professional.getUsername(),
                AccountRole.PROFESSIONAL, professional.getTokenVersion(), false);
        String taskId = "rc2-pipeline-" + suffix;
        AgentTask queued = orchestrator.submit(principal, AgentCapability.MULTI_AGENT,
                2_000, 60, "结合文昌红树林蓝碳指标趋势以及核证方法来源形成报告", taskId);
        assertEquals(AgentTaskStatus.QUEUED, queued.getStatus());

        AgentTask completed = awaitTerminal(taskId, Duration.ofSeconds(60));
        assertEquals(AgentTaskStatus.SUCCEEDED, completed.getStatus(), completed.getErrorMessage());
        assertEquals(1, completed.getAttempts());
        assertNotNull(completed.getActualTokens());
        assertTrue(completed.getActualTokens() > 0);

        AgentGatewayContract.Result result = mapper.readValue(
                completed.getResultJson(), AgentGatewayContract.Result.class);
        assertTrue(result.success());
        assertFalse(result.partial());
        assertNotNull(result.output());
        assertFalse(result.output().isBlank());
        assertFalse(result.sources().isEmpty());
        assertNotNull(result.routingAudit());
        assertEquals("JAVA_POLICY_AND_EXISTING_RULES", result.routingAudit().authority());
        assertEquals("SHADOW", result.routingAudit().mode());
        assertTrue(result.sources().stream().anyMatch(source ->
                "HNBLUE_RAG_3_0_LOCAL_INDEX".equals(source.get("datasetIdentity"))));
        boolean allowSyntheticFixture = Boolean.getBoolean("hnblue.pipeline.allow-synthetic-fixture");
        if (allowSyntheticFixture) {
            assertTrue(result.sources().stream().anyMatch(source ->
                    "HNBLUE_JAVA_V2_CURRENT_DATABASE".equals(source.get("datasetIdentity"))
                            && Boolean.TRUE.equals(source.get("synthetic"))),
                    "isolated Stage 3 pipeline must preserve the synthetic Java V2 marker");
        } else {
            assertTrue(result.sources().stream().anyMatch(source ->
                    "HNBLUE_JAVA_V2_CURRENT_DATABASE".equals(source.get("datasetIdentity"))
                            && !Boolean.TRUE.equals(source.get("synthetic"))),
                    "real pipeline must include non-synthetic Java V2 current-database evidence");
        }

        AgentDispatchOutbox outbox = awaitOutboxConsumed(taskId, Duration.ofSeconds(10));
        assertEquals(AgentDispatchOutbox.Status.CONSUMED, outbox.getStatus());
        assertEquals(1, outbox.getAttempts());
        assertNotNull(outbox.getPublishedAt());
        assertNotNull(outbox.getConsumedAt());
        assertEquals(AgentQuotaReservation.Status.SETTLED,
                reservations.findById(completed.getReservationId()).orElseThrow().getStatus());
        assertTrue(redis.opsForStream().size(STREAM) >= 1);
        assertEquals(0L, redis.opsForStream().pending(STREAM, GROUP).getTotalPendingMessages());
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
        throw new AssertionError("real agent pipeline did not finish within " + timeout);
    }

    private AgentDispatchOutbox awaitOutboxConsumed(String taskId, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        AgentDispatchOutbox last = null;
        while (Instant.now().isBefore(deadline)) {
            last = outboxes.findByTaskId(taskId).orElse(null);
            if (last != null && last.getStatus() == AgentDispatchOutbox.Status.CONSUMED) {
                return last;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("outbox did not become CONSUMED within " + timeout
                + "; last status=" + (last == null ? "missing" : last.getStatus()));
    }
}
