package com.example.jpaspringboot.agent;

import com.example.jpaspringboot.JpAspringbootApplication;
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
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "hnblue.restart.recovery.it", matches = "true")
class RealApplicationRestartRecoveryIntegrationTest {
    @Test
    void persistsAndRecoversTaskAcrossTwoApplicationContexts() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String stream = "hnblue:rc3:it:restart:" + suffix;
        String group = "hnblue-rc3-restart-workers-" + suffix;
        String taskId = "rc3-restart-" + suffix;

        try (ConfigurableApplicationContext first = start(stream, group, "before-restart")) {
            ProfessionalAccountRepository professionals = first.getBean(ProfessionalAccountRepository.class);
            AgentEntitlementRepository entitlements = first.getBean(AgentEntitlementRepository.class);
            AgentTaskOrchestrator orchestrator = first.getBean(AgentTaskOrchestrator.class);
            RedisAgentDispatchService dispatch = first.getBean(RedisAgentDispatchService.class);
            AgentTaskService tasks = first.getBean(AgentTaskService.class);
            AgentDispatchOutboxRepository outboxes = first.getBean(AgentDispatchOutboxRepository.class);

            ProfessionalAccount account = new ProfessionalAccount();
            account.setUsername("rc3-restart-user-" + suffix);
            account.setPasswordHash("{noop}local-test-only");
            account.setEnabled(true);
            account.setPasswordChangeRequired(false);
            account.setCreatedBy("RC3_TEST");
            account = professionals.saveAndFlush(account);

            AgentEntitlement entitlement = new AgentEntitlement();
            entitlement.setProfessionalAccountId(account.getId());
            entitlement.setDailyRequestLimit(10);
            entitlement.setDailyTokenLimit(10_000);
            entitlement.setMaxTokensPerRequest(1_000);
            entitlements.saveAndFlush(entitlement);

            HnbluePrincipal principal = new HnbluePrincipal(account.getId(), account.getUsername(),
                    AccountRole.PROFESSIONAL, account.getTokenVersion(), false);
            orchestrator.submit(principal, AgentCapability.BASIC_QA, 100, 120,
                    "survive application restart", taskId);
            dispatch.publishAvailable();
            AgentTask running = tasks.markRunning(taskId);
            assertEquals(AgentTaskStatus.RUNNING, running.getStatus());
            AgentDispatchOutbox outbox = outboxes.findByTaskId(taskId).orElseThrow();
            outbox.setAvailableAt(LocalDateTime.now().minusSeconds(1));
            outboxes.saveAndFlush(outbox);
        }

        try (ConfigurableApplicationContext second = start(stream, group, "after-restart")) {
            RedisAgentDispatchService dispatch = second.getBean(RedisAgentDispatchService.class);
            AgentTaskRepository tasks = second.getBean(AgentTaskRepository.class);
            AgentDispatchOutboxRepository outboxes = second.getBean(AgentDispatchOutboxRepository.class);
            AgentQuotaReservationRepository reservations = second.getBean(AgentQuotaReservationRepository.class);
            StringRedisTemplate redis = second.getBean(StringRedisTemplate.class);

            dispatch.publishAvailable();
            assertEquals(AgentTaskStatus.QUEUED, tasks.findById(taskId).orElseThrow().getStatus());
            assertEquals(2, outboxes.findByTaskId(taskId).orElseThrow().getAttempts());

            try {
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while aging pending entry", ex);
            }
            dispatch.consumeAvailable();
            dispatch.consumeAvailable();
            AgentTask terminal = tasks.findById(taskId).orElseThrow();
            assertEquals(AgentTaskStatus.FAILED, terminal.getStatus());
            assertEquals("gateway_disabled", terminal.getErrorCode());
            assertEquals(2, terminal.getAttempts());
            assertEquals(AgentQuotaReservation.Status.RELEASED,
                    reservations.findById(terminal.getReservationId()).orElseThrow().getStatus());
            assertEquals(AgentDispatchOutbox.Status.CONSUMED,
                    outboxes.findByTaskId(taskId).orElseThrow().getStatus());
            assertEquals(0L, redis.opsForStream().pending(stream, group).getTotalPendingMessages());
            assertTrue(redis.opsForStream().size(stream) >= 2);
        }
    }

    private ConfigurableApplicationContext start(String stream, String group, String consumer) {
        String datasourceUrl = required("spring.datasource.url");
        String datasourceUsername = required("spring.datasource.username");
        String datasourcePassword = System.getProperty("spring.datasource.password", "");
        String redisHost = System.getProperty("spring.data.redis.host", "127.0.0.1");
        String redisPort = System.getProperty("spring.data.redis.port", "36379");
        String redisPassword = required("spring.data.redis.password");
        return new SpringApplicationBuilder(JpAspringbootApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + datasourceUrl,
                        "--spring.datasource.username=" + datasourceUsername,
                        "--spring.datasource.password=" + datasourcePassword,
                        "--spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver",
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
                        "--spring.jpa.defer-datasource-initialization=false",
                        "--spring.sql.init.mode=never",
                        "--spring.flyway.enabled=false",
                        "--spring.data.redis.host=" + redisHost,
                        "--spring.data.redis.port=" + redisPort,
                        "--spring.data.redis.password=" + redisPassword,
                        "--spring.data.redis.timeout=3s",
                        "--spring.data.redis.repositories.enabled=false",
                        "--management.health.redis.enabled=false",
                        "--hnblue.agent.dispatch.mode=redis-stream",
                        "--hnblue.agent.gateway.enabled=false",
                        "--hnblue.agent.redis.stream=" + stream,
                        "--hnblue.agent.redis.group=" + group,
                        "--hnblue.agent.redis.consumer=" + consumer,
                        "--hnblue.agent.redis.publish-ms=3600000",
                        "--hnblue.agent.redis.consume-ms=3600000",
                        "--hnblue.agent.redis.retry-after=1ms",
                        "--hnblue.agent.redis.max-attempts=3",
                        "--hnblue.agent.timeout-scan-ms=3600000"
                );
    }

    private String required(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException(key + " is required");
        return value;
    }
}
