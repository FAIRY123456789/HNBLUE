-- Destructive rollback reference only. Never execute against production
-- without a backup and explicit approval.
ALTER TABLE agent_task
    DROP COLUMN input_message,
    DROP COLUMN password_change_required,
    DROP COLUMN owner_token_version,
    DROP COLUMN owner_username;

ALTER TABLE agent_dispatch_outbox
    DROP INDEX idx_agent_outbox_status_available,
    DROP INDEX uk_agent_outbox_task,
    CHANGE COLUMN available_at next_attempt_at DATETIME(6) NOT NULL,
    MODIFY COLUMN payload_json TEXT NOT NULL,
    ADD COLUMN event_type VARCHAR(48) NOT NULL DEFAULT 'AGENT_TASK_QUEUED' AFTER task_id,
    DROP COLUMN consumed_at,
    DROP COLUMN updated_at,
    DROP COLUMN version,
    ADD KEY idx_outbox_status_next (status, next_attempt_at);

RENAME TABLE agent_dispatch_outbox TO agent_task_outbox;
