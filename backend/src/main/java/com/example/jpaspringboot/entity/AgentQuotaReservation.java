package com.example.jpaspringboot.entity;

import com.example.jpaspringboot.agent.AgentCapability;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_quota_reservation")
public class AgentQuotaReservation {
    public enum Status { RESERVED, SETTLED, RELEASED }

    @Id
    @Column(name = "reservation_id", length = 64)
    private String reservationId;
    @Column(name = "subject_key", nullable = false, length = 96)
    private String subjectKey;
    @Column(name = "quota_day", nullable = false)
    private LocalDate quotaDay;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentCapability capability;
    @Column(name = "reserved_tokens", nullable = false)
    private int reservedTokens;
    @Column(name = "actual_tokens")
    private Integer actualTokens;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void created() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getSubjectKey() { return subjectKey; }
    public void setSubjectKey(String subjectKey) { this.subjectKey = subjectKey; }
    public LocalDate getQuotaDay() { return quotaDay; }
    public void setQuotaDay(LocalDate quotaDay) { this.quotaDay = quotaDay; }
    public AgentCapability getCapability() { return capability; }
    public void setCapability(AgentCapability capability) { this.capability = capability; }
    public int getReservedTokens() { return reservedTokens; }
    public void setReservedTokens(int reservedTokens) { this.reservedTokens = reservedTokens; }
    public Integer getActualTokens() { return actualTokens; }
    public void setActualTokens(Integer actualTokens) { this.actualTokens = actualTokens; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
