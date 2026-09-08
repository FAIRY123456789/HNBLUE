-- HNBLUE local controlled migration: normalize and deduplicate user.email, then enforce uniqueness.
-- Database: hnblue_v2_dev_control
-- Do not store credentials in this file. Run only after reviewing duplicate groups.

START TRANSACTION;

CREATE TABLE IF NOT EXISTS user_email_dedup_backup_20260716 (
  backup_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  username VARCHAR(255),
  original_email VARCHAR(255),
  normalized_email VARCHAR(255),
  backup_time DATETIME NOT NULL,
  UNIQUE KEY uk_backup_user_id (user_id)
);

CREATE TEMPORARY TABLE tmp_hnblue_email_plan AS
SELECT
  id AS user_id,
  name AS username,
  email AS original_email,
  CASE
    WHEN email IS NULL OR TRIM(email) = '' THEN NULL
    ELSE LOWER(TRIM(email))
  END AS normalized_email,
  ROW_NUMBER() OVER (PARTITION BY LOWER(TRIM(email)) ORDER BY id) AS rn,
  COUNT(*) OVER (PARTITION BY LOWER(TRIM(email))) AS duplicate_count
FROM user;

INSERT INTO user_email_dedup_backup_20260716 (user_id, username, original_email, normalized_email, backup_time)
SELECT
  user_id,
  username,
  original_email,
  CASE
    WHEN normalized_email IS NULL THEN NULL
    WHEN duplicate_count > 1 AND rn > 1 THEN CONCAT(
      LEFT(SUBSTRING_INDEX(normalized_email, '@', 1),
        255 - LENGTH(CONCAT('_u', user_id)) - 1 - LENGTH(SUBSTRING_INDEX(normalized_email, '@', -1))
      ),
      '_u', user_id, '@', SUBSTRING_INDEX(normalized_email, '@', -1)
    )
    ELSE normalized_email
  END,
  NOW()
FROM tmp_hnblue_email_plan
WHERE NOT (original_email <=> CASE
  WHEN normalized_email IS NULL THEN NULL
  WHEN duplicate_count > 1 AND rn > 1 THEN CONCAT(
    LEFT(SUBSTRING_INDEX(normalized_email, '@', 1),
      255 - LENGTH(CONCAT('_u', user_id)) - 1 - LENGTH(SUBSTRING_INDEX(normalized_email, '@', -1))
    ),
    '_u', user_id, '@', SUBSTRING_INDEX(normalized_email, '@', -1)
  )
  ELSE normalized_email
END)
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  original_email = VALUES(original_email),
  normalized_email = VALUES(normalized_email),
  backup_time = VALUES(backup_time);

UPDATE user u
JOIN user_email_dedup_backup_20260716 b ON b.user_id = u.id
SET u.email = b.normalized_email
WHERE NOT (u.email <=> b.normalized_email);

-- Validation before COMMIT:
-- SELECT COUNT(*) FROM user;
-- SELECT LOWER(TRIM(email)), COUNT(*) FROM user WHERE email IS NOT NULL AND TRIM(email) <> '' GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1;

COMMIT;

-- MySQL DDL causes implicit commit. Run after the validation query above returns no duplicate rows.
ALTER TABLE user ADD UNIQUE KEY uk_user_email (email);
ALTER TABLE user ADD UNIQUE KEY uk_user_username (name);

-- Rollback strategy after commit, using backup table:
-- UPDATE user u JOIN user_email_dedup_backup_20260716 b ON b.user_id = u.id SET u.email = b.original_email;
-- ALTER TABLE user DROP INDEX uk_user_email;
-- ALTER TABLE user DROP INDEX uk_user_username;
