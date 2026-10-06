package com.example.jpaspringboot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_message", indexes = {
        @Index(name = "idx_ai_chat_owner_session_time", columnList = "owner_key,session_id,created_at"),
        @Index(name = "idx_ai_chat_created", columnList = "created_at")
})
public class AiChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_key", nullable = false, length = 191)
    private String ownerKey;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false, length = 16)
    private String role;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Lob
    @Column(name = "sources_json", columnDefinition = "LONGTEXT")
    private String sourcesJson;

    @Column(name = "evidence_status", length = 48)
    private String evidenceStatus;

    @Column(name = "model_name", length = 80)
    private String modelName;

    @Column(name = "injection_risk", length = 16)
    private String injectionRisk;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getOwnerKey() { return ownerKey; }
    public void setOwnerKey(String ownerKey) { this.ownerKey = ownerKey; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSourcesJson() { return sourcesJson; }
    public void setSourcesJson(String sourcesJson) { this.sourcesJson = sourcesJson; }
    public String getEvidenceStatus() { return evidenceStatus; }
    public void setEvidenceStatus(String evidenceStatus) { this.evidenceStatus = evidenceStatus; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getInjectionRisk() { return injectionRisk; }
    public void setInjectionRisk(String injectionRisk) { this.injectionRisk = injectionRisk; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
