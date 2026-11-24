package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.EntitlementPlan;
import com.example.jpaspringboot.entity.AgentQuotaLedger;
import com.example.jpaspringboot.entity.AgentQuotaReservation;
import com.example.jpaspringboot.repository.AgentQuotaLedgerRepository;
import com.example.jpaspringboot.repository.AgentQuotaReservationRepository;
import com.example.jpaspringboot.security.HnbluePrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AgentQuotaService {
    private final AgentEntitlementService entitlementService;
    private final AgentQuotaLedgerRepository ledgers;
    private final AgentQuotaReservationRepository reservations;

    public AgentQuotaService(AgentEntitlementService entitlementService,
                             AgentQuotaLedgerRepository ledgers,
                             AgentQuotaReservationRepository reservations) {
        this.entitlementService = entitlementService;
        this.ledgers = ledgers;
        this.reservations = reservations;
    }

    @Transactional
    public AgentQuotaReservation reserve(HnbluePrincipal principal, AgentCapability capability,
                                         int requestedTokens, String reservationId) {
        AgentQuotaReservation existing = reservations.findById(reservationId).orElse(null);
        if (existing != null) {
            if (!existing.getSubjectKey().equals(principal.subjectKey())) {
                throw new QuotaExceededException("idempotency_conflict", "reservation belongs to another subject");
            }
            return existing;
        }
        if (principal.passwordChangeRequired()) {
            throw new QuotaExceededException("password_change_required", "change the temporary password first");
        }
        EntitlementPlan plan = entitlementService.planFor(principal);
        if (!plan.permits(capability)) throw new QuotaExceededException("capability_forbidden", "capability is not enabled");
        if (requestedTokens < 1 || requestedTokens > plan.maxTokensPerRequest()) {
            throw new QuotaExceededException("request_token_limit", "requested token budget exceeds the per-request limit");
        }
        LocalDate day = LocalDate.now();
        int claimed = reservations.insertIfAbsent(reservationId, principal.subjectKey(), day,
                capability.name(), requestedTokens);
        if (claimed == 0) {
            AgentQuotaReservation concurrent = reservations.findById(reservationId)
                    .orElseThrow(() -> new IllegalStateException("reservation claim disappeared"));
            if (!concurrent.getSubjectKey().equals(principal.subjectKey())) {
                throw new QuotaExceededException("idempotency_conflict", "reservation belongs to another subject");
            }
            return concurrent;
        }

        ledgers.insertIfAbsent(principal.subjectKey(), day);
        AgentQuotaLedger ledger = ledgers.findBySubjectKeyAndQuotaDay(principal.subjectKey(), day)
                .orElseThrow(() -> new IllegalStateException("quota ledger initialization failed"));
        if (ledger.getRequestsUsed() + 1 > plan.dailyRequestLimit())
            throw new QuotaExceededException("daily_request_limit", "daily request quota exhausted");
        if (ledger.getTokensUsed() + requestedTokens > plan.dailyTokenLimit())
            throw new QuotaExceededException("daily_token_limit", "daily token quota exhausted");
        if (ledger.getConcurrentTasks() + 1 > plan.maxConcurrentTasks())
            throw new QuotaExceededException("concurrent_task_limit", "too many concurrent tasks");

        ledger.setRequestsUsed(ledger.getRequestsUsed() + 1);
        ledger.setTokensUsed(ledger.getTokensUsed() + requestedTokens);
        ledger.setConcurrentTasks(ledger.getConcurrentTasks() + 1);
        ledgers.save(ledger);

        return reservations.findById(reservationId)
                .orElseThrow(() -> new IllegalStateException("reservation initialization failed"));
    }

    @Transactional
    public void settle(String reservationId, int actualTokens) {
        AgentQuotaReservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("reservation not found"));
        if (reservation.getStatus() != AgentQuotaReservation.Status.RESERVED) return;
        AgentQuotaLedger ledger = requireLedger(reservation);
        int normalized = Math.max(0, actualTokens);
        ledger.setTokensUsed(Math.max(0, ledger.getTokensUsed() - reservation.getReservedTokens() + normalized));
        ledger.setConcurrentTasks(Math.max(0, ledger.getConcurrentTasks() - 1));
        reservation.setActualTokens(normalized);
        reservation.setStatus(AgentQuotaReservation.Status.SETTLED);
        ledgers.save(ledger);
        reservations.save(reservation);
    }

    @Transactional
    public void release(String reservationId) {
        AgentQuotaReservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("reservation not found"));
        if (reservation.getStatus() != AgentQuotaReservation.Status.RESERVED) return;
        AgentQuotaLedger ledger = requireLedger(reservation);
        ledger.setRequestsUsed(Math.max(0, ledger.getRequestsUsed() - 1));
        ledger.setTokensUsed(Math.max(0, ledger.getTokensUsed() - reservation.getReservedTokens()));
        ledger.setConcurrentTasks(Math.max(0, ledger.getConcurrentTasks() - 1));
        reservation.setStatus(AgentQuotaReservation.Status.RELEASED);
        ledgers.save(ledger);
        reservations.save(reservation);
    }

    private AgentQuotaLedger requireLedger(AgentQuotaReservation reservation) {
        return ledgers.findBySubjectKeyAndQuotaDay(reservation.getSubjectKey(), reservation.getQuotaDay())
                .orElseThrow(() -> new IllegalStateException("quota ledger missing"));
    }

}
