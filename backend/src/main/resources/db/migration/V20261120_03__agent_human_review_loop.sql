ALTER TABLE agent_task
    ADD COLUMN review_level VARCHAR(32) NULL AFTER error_message,
    ADD COLUMN review_cycle INT NOT NULL DEFAULT 0 AFTER review_level;

CREATE TABLE agent_task_review_action (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    action VARCHAR(16) NOT NULL,
    reviewer_key VARCHAR(96) NOT NULL,
    reviewer_role VARCHAR(32) NOT NULL,
    review_cycle INT NOT NULL,
    from_status VARCHAR(24) NOT NULL,
    to_status VARCHAR(24) NOT NULL,
    comment VARCHAR(2000) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_agent_review_request UNIQUE (request_id),
    INDEX idx_agent_review_task_created (task_id, created_at)
);
