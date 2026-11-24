package com.example.jpaspringboot.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_quota_ledger", uniqueConstraints =
        @UniqueConstraint(name = "uk_quota_subject_day", columnNames = {"subject_key", "quota_day"}))
public class AgentQuotaLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "subject_key", nullable = false, length = 96)
    private String subjectKey;
    @Column(name = "quota_day", nullable = false)
    private LocalDate quotaDay;
    @Column(name = "requests_used", nullable = false)
    private int requestsUsed;
    @Column(name = "tokens_used", nullable = false)
    private long tokensUsed;
    @Column(name = "concurrent_tasks", nullable = false)
    private int concurrentTasks;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Version
    private long version;

    @PrePersist @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getSubjectKey() { return subjectKey; }
    public void setSubjectKey(String subjectKey) { this.subjectKey = subjectKey; }
    public LocalDate getQuotaDay() { return quotaDay; }
    public void setQuotaDay(LocalDate quotaDay) { this.quotaDay = quotaDay; }
    public int getRequestsUsed() { return requestsUsed; }
    public void setRequestsUsed(int requestsUsed) { this.requestsUsed = requestsUsed; }
    public long getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(long tokensUsed) { this.tokensUsed = tokensUsed; }
    public int getConcurrentTasks() { return concurrentTasks; }
    public void setConcurrentTasks(int concurrentTasks) { this.concurrentTasks = concurrentTasks; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
