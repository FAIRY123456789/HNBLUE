package com.example.jpaspringboot.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_dispatch_outbox", indexes = {
        @Index(name = "idx_agent_outbox_status_available", columnList = "status,available_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_agent_outbox_task", columnNames = "task_id"))
public class AgentDispatchOutbox {
    public enum Status { PENDING, PUBLISHED, CONSUMED, DEAD }

    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;
    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;
    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;
    @Column(name = "last_error", length = 600)
    private String lastError;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Version
    private long version;

    @PrePersist
    void created() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (availableAt == null) availableAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updated() { updatedAt = LocalDateTime.now(); }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getAvailableAt() { return availableAt; }
    public void setAvailableAt(LocalDateTime availableAt) { this.availableAt = availableAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
