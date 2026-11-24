package com.example.jpaspringboot.entity;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.AgentTaskStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_task", indexes = {
        @Index(name = "idx_agent_task_owner_created", columnList = "owner_key,created_at"),
        @Index(name = "idx_agent_task_status_deadline", columnList = "status,deadline")
})
public class AgentTask {
    @Id
    @Column(name = "task_id", length = 64)
    private String taskId;
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;
    @Column(name = "owner_key", nullable = false, length = 96)
    private String ownerKey;
    @Column(name = "owner_role", nullable = false, length = 32)
    private String ownerRole;
    @Column(name = "owner_username", length = 64)
    private String ownerUsername;
    @Column(name = "owner_token_version")
    private Long ownerTokenVersion;
    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;
    @Lob
    @Column(name = "input_message", columnDefinition = "LONGTEXT")
    private String inputMessage;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentCapability capability;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AgentTaskStatus status;
    @Column(name = "reservation_id", nullable = false, length = 64, unique = true)
    private String reservationId;
    @Column(name = "requested_tokens", nullable = false)
    private int requestedTokens;
    @Column(name = "actual_tokens")
    private Integer actualTokens;
    @Column(nullable = false)
    private int priority;
    @Column(nullable = false)
    private int attempts;
    @Column(nullable = false)
    private LocalDateTime deadline;
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;
    @Column(name = "error_code", length = 80)
    private String errorCode;
    @Column(name = "error_message", length = 600)
    private String errorMessage;
    @Column(name = "review_level", length = 32)
    private String reviewLevel;
    @Column(name = "review_cycle", nullable = false)
    private int reviewCycle;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Version
    private long version;

    @PrePersist void created() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getOwnerKey() { return ownerKey; }
    public void setOwnerKey(String ownerKey) { this.ownerKey = ownerKey; }
    public String getOwnerRole() { return ownerRole; }
    public void setOwnerRole(String ownerRole) { this.ownerRole = ownerRole; }
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public Long getOwnerTokenVersion() { return ownerTokenVersion; }
    public void setOwnerTokenVersion(Long ownerTokenVersion) { this.ownerTokenVersion = ownerTokenVersion; }
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
    public void setPasswordChangeRequired(boolean passwordChangeRequired) { this.passwordChangeRequired = passwordChangeRequired; }
    public String getInputMessage() { return inputMessage; }
    public void setInputMessage(String inputMessage) { this.inputMessage = inputMessage; }
    public AgentCapability getCapability() { return capability; }
    public void setCapability(AgentCapability capability) { this.capability = capability; }
    public AgentTaskStatus getStatus() { return status; }
    public void setStatus(AgentTaskStatus status) { this.status = status; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public int getRequestedTokens() { return requestedTokens; }
    public void setRequestedTokens(int requestedTokens) { this.requestedTokens = requestedTokens; }
    public Integer getActualTokens() { return actualTokens; }
    public void setActualTokens(Integer actualTokens) { this.actualTokens = actualTokens; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getReviewLevel() { return reviewLevel; }
    public void setReviewLevel(String reviewLevel) { this.reviewLevel = reviewLevel; }
    public int getReviewCycle() { return reviewCycle; }
    public void setReviewCycle(int reviewCycle) { this.reviewCycle = reviewCycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
