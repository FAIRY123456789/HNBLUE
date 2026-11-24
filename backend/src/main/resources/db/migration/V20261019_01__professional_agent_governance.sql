-- HNBLUE phase-one additive migration. Review against a schema-only export first.
-- No existing ADMIN/PUBLIC rows are updated or deleted.

CREATE TABLE professional_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    token_version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    last_login_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_professional_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_entitlement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    professional_account_id BIGINT NOT NULL,
    basic_qa_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    professional_analysis_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    multi_agent_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    daily_request_limit INT NOT NULL DEFAULT 100,
    daily_token_limit BIGINT NOT NULL DEFAULT 200000,
    max_tokens_per_request INT NOT NULL DEFAULT 8000,
    max_concurrent_tasks INT NOT NULL DEFAULT 2,
    queue_priority INT NOT NULL DEFAULT 10,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_entitlement_professional UNIQUE (professional_account_id),
    CONSTRAINT fk_entitlement_professional FOREIGN KEY (professional_account_id)
        REFERENCES professional_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_quota_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject_key VARCHAR(96) NOT NULL,
    quota_day DATE NOT NULL,
    requests_used INT NOT NULL DEFAULT 0,
    tokens_used BIGINT NOT NULL DEFAULT 0,
    concurrent_tasks INT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_quota_subject_day UNIQUE (subject_key, quota_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_quota_reservation (
    reservation_id VARCHAR(64) NOT NULL,
    subject_key VARCHAR(96) NOT NULL,
    quota_day DATE NOT NULL,
    capability VARCHAR(32) NOT NULL,
    reserved_tokens INT NOT NULL,
    actual_tokens INT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (reservation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_task (
    task_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    owner_key VARCHAR(96) NOT NULL,
    owner_role VARCHAR(32) NOT NULL,
    capability VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL,
    reservation_id VARCHAR(64) NOT NULL,
    requested_tokens INT NOT NULL,
    actual_tokens INT NULL,
    priority INT NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    deadline DATETIME(6) NOT NULL,
    result_json TEXT NULL,
    error_code VARCHAR(80) NULL,
    error_message VARCHAR(600) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (task_id),
    CONSTRAINT uk_agent_task_reservation UNIQUE (reservation_id),
    KEY idx_agent_task_owner_created (owner_key, created_at),
    KEY idx_agent_task_status_deadline (status, deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_task_outbox (
    event_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    last_error VARCHAR(600) NULL,
    PRIMARY KEY (event_id),
    KEY idx_outbox_status_next (status, next_attempt_at),
    CONSTRAINT fk_outbox_task FOREIGN KEY (task_id) REFERENCES agent_task(task_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
