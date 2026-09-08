-- HNBLUE 核心功能与用户权限验收：数据库最小变更说明
-- 目的：支持最近登录时间与用户操作日志。
-- 原则：幂等新增，不删除或重建已有表；不存储明文密码、密码哈希、盐值、Token 或 API Key。

ALTER TABLE user
  ADD COLUMN IF NOT EXISTS last_login_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS user_activity_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_user_id INT NULL,
  actor_name VARCHAR(255) NULL,
  actor_role VARCHAR(64) NULL,
  target_user_id INT NULL,
  action_type VARCHAR(128) NULL,
  action_description VARCHAR(600) NULL,
  request_path VARCHAR(255) NULL,
  result_status VARCHAR(64) NULL,
  ip_address VARCHAR(128) NULL,
  occurred_at DATETIME NULL,
  INDEX idx_user_activity_target_time (target_user_id, occurred_at),
  INDEX idx_user_activity_actor_time (actor_user_id, occurred_at)
);
