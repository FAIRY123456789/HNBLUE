package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.AgentQuotaReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface AgentQuotaReservationRepository extends JpaRepository<AgentQuotaReservation, String> {
    @Modifying
    @Query(value = "INSERT IGNORE INTO agent_quota_reservation " +
            "(reservation_id, subject_key, quota_day, capability, reserved_tokens, actual_tokens, status, created_at, updated_at) " +
            "VALUES (:reservationId, :subjectKey, :quotaDay, :capability, :reservedTokens, NULL, 'RESERVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            nativeQuery = true)
    int insertIfAbsent(@Param("reservationId") String reservationId,
                       @Param("subjectKey") String subjectKey,
                       @Param("quotaDay") LocalDate quotaDay,
                       @Param("capability") String capability,
                       @Param("reservedTokens") int reservedTokens);
}
