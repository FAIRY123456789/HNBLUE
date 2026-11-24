-- Run only after backing up review audit records and receiving explicit approval.
DROP TABLE IF EXISTS agent_task_review_action;
ALTER TABLE agent_task
    DROP COLUMN review_cycle,
    DROP COLUMN review_level;
