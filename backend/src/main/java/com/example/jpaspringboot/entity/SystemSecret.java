package com.example.jpaspringboot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_secret", uniqueConstraints = {
        @UniqueConstraint(name = "uk_system_secret_name", columnNames = "secret_name")
})
public class SystemSecret {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "secret_name", nullable = false, length = 80)
    private String secretName;

    @Lob
    @Column(name = "cipher_text", nullable = false, columnDefinition = "LONGTEXT")
    private String cipherText;

    @Column(name = "initialization_vector", nullable = false, length = 64)
    private String initializationVector;

    @Column(name = "last_four", nullable = false, length = 8)
    private String lastFour;

    @Column(name = "key_version", nullable = false)
    private int keyVersion = 1;

    @Column(name = "updated_by", nullable = false, length = 191)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public String getSecretName() { return secretName; }
    public void setSecretName(String secretName) { this.secretName = secretName; }
    public String getCipherText() { return cipherText; }
    public void setCipherText(String cipherText) { this.cipherText = cipherText; }
    public String getInitializationVector() { return initializationVector; }
    public void setInitializationVector(String initializationVector) { this.initializationVector = initializationVector; }
    public String getLastFour() { return lastFour; }
    public void setLastFour(String lastFour) { this.lastFour = lastFour; }
    public int getKeyVersion() { return keyVersion; }
    public void setKeyVersion(int keyVersion) { this.keyVersion = keyVersion; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
