package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.AgentQuotaLedger;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AgentQuotaLedgerRepository extends JpaRepository<AgentQuotaLedger, Long> {
    @Modifying
    @Query(value = "INSERT IGNORE INTO agent_quota_ledger " +
            "(subject_key, quota_day, requests_used, tokens_used, concurrent_tasks, updated_at, version) " +
            "VALUES (:subjectKey, :quotaDay, 0, 0, 0, CURRENT_TIMESTAMP, 0)", nativeQuery = true)
    int insertIfAbsent(@Param("subjectKey") String subjectKey, @Param("quotaDay") LocalDate quotaDay);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AgentQuotaLedger> findBySubjectKeyAndQuotaDay(String subjectKey, LocalDate quotaDay);
}
