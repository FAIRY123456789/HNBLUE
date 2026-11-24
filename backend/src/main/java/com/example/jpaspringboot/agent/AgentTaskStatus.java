package com.example.jpaspringboot.agent;

public enum AgentTaskStatus {
    CREATED, QUEUED, RUNNING,
    AWAITING_REVIEW, AWAITING_ARCHIVE,
    SUCCEEDED, PARTIAL, REJECTED, ARCHIVED, FAILED, CANCELLED
}
