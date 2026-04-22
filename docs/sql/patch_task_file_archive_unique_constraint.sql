-- =========================================================
-- Patch: add unique constraint for task input archive contract
-- Reason:
--   A task should keep only one effective input archive
--   for the same (task_id, file_role, file_key).
--   This patch removes duplicate historical rows first,
--   then adds a rerunnable unique constraint.
-- Target database:
--   task_db
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE task_db;

DELETE tf_old
FROM task_file tf_old
INNER JOIN task_file tf_new
        ON tf_old.task_id = tf_new.task_id
       AND tf_old.file_role = tf_new.file_role
       AND tf_old.file_key = tf_new.file_key
       AND tf_old.id < tf_new.id;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = 'task_db'
              AND TABLE_NAME = 'task_file'
              AND INDEX_NAME = 'uk_task_file_role_key'
        ),
        'SELECT 1',
        'ALTER TABLE task_file ADD UNIQUE KEY uk_task_file_role_key (task_id, file_role, file_key)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
