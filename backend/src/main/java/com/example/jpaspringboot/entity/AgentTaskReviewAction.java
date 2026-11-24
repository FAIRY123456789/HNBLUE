package com.example.jpaspringboot.entity;

import com.example.jpaspringboot.agent.AgentReviewActionType;
import com.example.jpaspringboot.agent.AgentTaskStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_task_review_action", indexes = {
        @Index(name = "idx_agent_review_task_created", columnList = "task_id,created_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_agent_review_request", columnNames = "request_id"))
public class AgentTaskReviewAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;
    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AgentReviewActionType action;
    @Column(name = "reviewer_key", nullable = false, length = 96)
    private String reviewerKey;
    @Column(name = "reviewer_role", nullable = false, length = 32)
    private String reviewerRole;
    @Column(name = "review_cycle", nullable = false)
    private int reviewCycle;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 24)
    private AgentTaskStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 24)
    private AgentTaskStatus toStatus;
    @Column(length = 2000)
    private String comment;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist void created() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public AgentReviewActionType getAction() { return action; }
    public void setAction(AgentReviewActionType action) { this.action = action; }
    public String getReviewerKey() { return reviewerKey; }
    public void setReviewerKey(String reviewerKey) { this.reviewerKey = reviewerKey; }
    public String getReviewerRole() { return reviewerRole; }
    public void setReviewerRole(String reviewerRole) { this.reviewerRole = reviewerRole; }
    public int getReviewCycle() { return reviewCycle; }
    public void setReviewCycle(int reviewCycle) { this.reviewCycle = reviewCycle; }
    public AgentTaskStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(AgentTaskStatus fromStatus) { this.fromStatus = fromStatus; }
    public AgentTaskStatus getToStatus() { return toStatus; }
    public void setToStatus(AgentTaskStatus toStatus) { this.toStatus = toStatus; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
