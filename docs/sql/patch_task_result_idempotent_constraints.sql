-- =========================================================
-- Patch: add idempotent unique constraints for task result reporting
-- Reason:
--   Node-agent may retry reporting logs and result files.
--   Database unique constraints are required to make
--   ON DUPLICATE KEY UPDATE work as designed.
-- Target database:
--   task_db
-- Note:
--   This patch is designed to be rerunnable.
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE task_db;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = 'task_db'
              AND TABLE_NAME = 'task_result_file'
              AND INDEX_NAME = 'uk_task_file_type_name'
        ),
        'SELECT 1',
        'ALTER TABLE task_result_file ADD UNIQUE KEY uk_task_file_type_name (task_id, file_type, file_name)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
