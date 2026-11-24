-- Roll back only objects introduced by V20261019_01.
-- Run only after confirming no required local Agent/professional data remains.
DROP TABLE IF EXISTS agent_task_outbox;
DROP TABLE IF EXISTS agent_task;
DROP TABLE IF EXISTS agent_quota_reservation;
DROP TABLE IF EXISTS agent_quota_ledger;
DROP TABLE IF EXISTS agent_entitlement;
DROP TABLE IF EXISTS professional_account;
