package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.JpAspringbootApplication;
import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.entity.AgentEntitlement;
import com.example.jpaspringboot.entity.AgentQuotaReservation;
import com.example.jpaspringboot.repository.AgentEntitlementRepository;
import com.example.jpaspringboot.repository.AgentQuotaLedgerRepository;
import com.example.jpaspringboot.repository.AgentQuotaReservationRepository;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.HnbluePrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = JpAspringbootApplication.class)
class AgentQuotaServiceTest {
    @Autowired private AgentQuotaService quotas;
    @Autowired private AgentQuotaLedgerRepository ledgers;
    @Autowired private AgentQuotaReservationRepository reservations;
    @Autowired private AgentEntitlementRepository entitlements;

    @BeforeEach
    void resetQuotaFixtures() {
        reservations.deleteAll();
        ledgers.deleteAll();
        entitlements.deleteAll();
    }

    @Test
    void reservationIsIdempotentAndConcurrentLimitIsReleased() {
        HnbluePrincipal principal = new HnbluePrincipal(501L, "public-quota", AccountRole.PUBLIC, 0, false);
        AgentQuotaReservation first = quotas.reserve(principal, AgentCapability.BASIC_QA, 1000, "quota-public-0001");
        AgentQuotaReservation duplicate = quotas.reserve(principal, AgentCapability.BASIC_QA, 1000, "quota-public-0001");
        assertEquals(first.getReservationId(), duplicate.getReservationId());

        QuotaExceededException blocked = assertThrows(QuotaExceededException.class,
                () -> quotas.reserve(principal, AgentCapability.BASIC_QA, 1000, "quota-public-0002"));
        assertEquals("concurrent_task_limit", blocked.getCode());

        quotas.release(first.getReservationId());
        AgentQuotaReservation afterRelease = quotas.reserve(principal, AgentCapability.BASIC_QA, 1000, "quota-public-0003");
        quotas.settle(afterRelease.getReservationId(), 300);
        assertEquals(AgentQuotaReservation.Status.SETTLED,
                reservations.findById(afterRelease.getReservationId()).orElseThrow().getStatus());
    }

    @Test
    void professionalCapabilitiesAndTemporaryPasswordGateAreEnforced() {
        HnbluePrincipal temporary = new HnbluePrincipal(777L, "professional-quota",
                AccountRole.PROFESSIONAL, 0, true);
        QuotaExceededException passwordGate = assertThrows(QuotaExceededException.class,
                () -> quotas.reserve(temporary, AgentCapability.BASIC_QA, 1000, "quota-prof-0001"));
        assertEquals("password_change_required", passwordGate.getCode());

        HnbluePrincipal active = new HnbluePrincipal(777L, "professional-quota",
                AccountRole.PROFESSIONAL, 1, false);
        QuotaExceededException capabilityGate = assertThrows(QuotaExceededException.class,
                () -> quotas.reserve(active, AgentCapability.MULTI_AGENT, 1000, "quota-prof-0002"));
        assertEquals("capability_forbidden", capabilityGate.getCode());

        AgentEntitlement entitlement = new AgentEntitlement();
        entitlement.setProfessionalAccountId(777L);
        entitlement.setMultiAgentEnabled(true);
        entitlements.save(entitlement);
        AgentQuotaReservation allowed = quotas.reserve(active, AgentCapability.MULTI_AGENT, 1000, "quota-prof-0003");
        assertEquals(AgentCapability.MULTI_AGENT, allowed.getCapability());
    }

    @Test
    void concurrentRetriesWithSameIdempotencyKeyConsumeQuotaOnce() throws Exception {
        HnbluePrincipal principal = new HnbluePrincipal(909L, "public-concurrent",
                AccountRole.PUBLIC, 0, false);
        int workers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<AgentQuotaReservation>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < workers; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return quotas.reserve(principal, AgentCapability.BASIC_QA, 500, "quota-concurrent-0001");
                }));
            }
            ready.await();
            start.countDown();
            for (Future<AgentQuotaReservation> future : futures) {
                assertEquals("quota-concurrent-0001", future.get().getReservationId());
            }
        } finally {
            pool.shutdownNow();
        }

        var ledger = ledgers.findAll().stream()
                .filter(item -> item.getSubjectKey().equals(principal.subjectKey()))
                .filter(item -> item.getQuotaDay().equals(LocalDate.now()))
                .findFirst().orElseThrow();
        assertEquals(1, ledger.getRequestsUsed());
        assertEquals(500, ledger.getTokensUsed());
        assertEquals(1, ledger.getConcurrentTasks());
        assertEquals(1, reservations.count());
    }
}
