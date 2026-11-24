package com.example.jpaspringboot.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_entitlement", uniqueConstraints =
        @UniqueConstraint(name = "uk_entitlement_professional", columnNames = "professional_account_id"))
public class AgentEntitlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professional_account_id", nullable = false)
    private Long professionalAccountId;

    @Column(name = "basic_qa_enabled", nullable = false)
    private boolean basicQaEnabled = true;

    @Column(name = "professional_analysis_enabled", nullable = false)
    private boolean professionalAnalysisEnabled = true;

    @Column(name = "multi_agent_enabled", nullable = false)
    private boolean multiAgentEnabled;

    @Column(name = "daily_request_limit", nullable = false)
    private int dailyRequestLimit = 100;

    @Column(name = "daily_token_limit", nullable = false)
    private long dailyTokenLimit = 200_000;

    @Column(name = "max_tokens_per_request", nullable = false)
    private int maxTokensPerRequest = 8_000;

    @Column(name = "max_concurrent_tasks", nullable = false)
    private int maxConcurrentTasks = 2;

    @Column(name = "queue_priority", nullable = false)
    private int queuePriority = 10;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getProfessionalAccountId() { return professionalAccountId; }
    public void setProfessionalAccountId(Long professionalAccountId) { this.professionalAccountId = professionalAccountId; }
    public boolean isBasicQaEnabled() { return basicQaEnabled; }
    public void setBasicQaEnabled(boolean basicQaEnabled) { this.basicQaEnabled = basicQaEnabled; }
    public boolean isProfessionalAnalysisEnabled() { return professionalAnalysisEnabled; }
    public void setProfessionalAnalysisEnabled(boolean professionalAnalysisEnabled) { this.professionalAnalysisEnabled = professionalAnalysisEnabled; }
    public boolean isMultiAgentEnabled() { return multiAgentEnabled; }
    public void setMultiAgentEnabled(boolean multiAgentEnabled) { this.multiAgentEnabled = multiAgentEnabled; }
    public int getDailyRequestLimit() { return dailyRequestLimit; }
    public void setDailyRequestLimit(int dailyRequestLimit) { this.dailyRequestLimit = dailyRequestLimit; }
    public long getDailyTokenLimit() { return dailyTokenLimit; }
    public void setDailyTokenLimit(long dailyTokenLimit) { this.dailyTokenLimit = dailyTokenLimit; }
    public int getMaxTokensPerRequest() { return maxTokensPerRequest; }
    public void setMaxTokensPerRequest(int maxTokensPerRequest) { this.maxTokensPerRequest = maxTokensPerRequest; }
    public int getMaxConcurrentTasks() { return maxConcurrentTasks; }
    public void setMaxConcurrentTasks(int maxConcurrentTasks) { this.maxConcurrentTasks = maxConcurrentTasks; }
    public int getQueuePriority() { return queuePriority; }
    public void setQueuePriority(int queuePriority) { this.queuePriority = queuePriority; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
