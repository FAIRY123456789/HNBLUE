-- RC3 aligns the RC2 draft outbox schema with the entity used by the service.
-- This version assumes the clean V20261019_01 Flyway lineage. Legacy schemas
-- created by Hibernate require the separately reviewed preflight/repair path.

RENAME TABLE agent_task_outbox TO agent_dispatch_outbox;

ALTER TABLE agent_dispatch_outbox
    DROP INDEX idx_outbox_status_next,
    CHANGE COLUMN next_attempt_at available_at DATETIME(6) NOT NULL,
    MODIFY COLUMN payload_json LONGTEXT NOT NULL,
    DROP COLUMN event_type,
    ADD COLUMN consumed_at DATETIME(6) NULL AFTER published_at,
    ADD COLUMN updated_at DATETIME(6) NULL AFTER created_at,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT uk_agent_outbox_task UNIQUE (task_id),
    ADD KEY idx_agent_outbox_status_available (status, available_at);

UPDATE agent_dispatch_outbox
   SET updated_at = COALESCE(published_at, created_at)
 WHERE updated_at IS NULL;

ALTER TABLE agent_dispatch_outbox
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL;

ALTER TABLE agent_task
    ADD COLUMN owner_username VARCHAR(64) NULL AFTER owner_role,
    ADD COLUMN owner_token_version BIGINT NULL AFTER owner_username,
    ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE AFTER owner_token_version,
    ADD COLUMN input_message LONGTEXT NULL AFTER password_change_required;
