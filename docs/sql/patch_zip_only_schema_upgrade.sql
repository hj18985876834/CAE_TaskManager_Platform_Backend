-- =========================================================
-- ZIP_ONLY schema upgrade patch
-- Target: MySQL 8.0+
-- Scope: solver_db, task_db
-- Note: This patch is designed to be rerunnable.
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------
-- 1) solver_db upgrades
-- ---------------------------------------------------------
USE solver_db;

-- solver_task_profile.upload_mode
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'solver_db'
              AND TABLE_NAME = 'solver_task_profile'
              AND COLUMN_NAME = 'upload_mode'
        ),
        'SELECT 1',
        'ALTER TABLE solver_task_profile ADD COLUMN upload_mode VARCHAR(30) NOT NULL DEFAULT ''ZIP_ONLY'' COMMENT ''上传模式：ZIP_ONLY'' AFTER profile_name'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill existing rows
UPDATE solver_task_profile
SET upload_mode = 'ZIP_ONLY'
WHERE upload_mode IS NULL OR upload_mode = '';

-- solver_profile_file_rule.path_pattern
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'solver_db'
              AND TABLE_NAME = 'solver_profile_file_rule'
              AND COLUMN_NAME = 'path_pattern'
        ),
        'SELECT 1',
        'ALTER TABLE solver_profile_file_rule ADD COLUMN path_pattern VARCHAR(255) DEFAULT NULL COMMENT ''解压后相对路径规则，如 system/**'' AFTER file_key'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- solver_profile_file_rule.rule_json
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'solver_db'
              AND TABLE_NAME = 'solver_profile_file_rule'
              AND COLUMN_NAME = 'rule_json'
        ),
        'SELECT 1',
        'ALTER TABLE solver_profile_file_rule ADD COLUMN rule_json TEXT DEFAULT NULL COMMENT ''扩展约束JSON（数量/后缀/大小）'' AFTER sort_order'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill path_pattern for existing rules
UPDATE solver_profile_file_rule
SET path_pattern = COALESCE(NULLIF(file_name_pattern, ''), file_key)
WHERE path_pattern IS NULL OR path_pattern = '';

-- ---------------------------------------------------------
-- 2) task_db upgrades
-- ---------------------------------------------------------
USE task_db;

-- task_file.unpack_dir
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'task_db'
              AND TABLE_NAME = 'task_file'
              AND COLUMN_NAME = 'unpack_dir'
        ),
        'SELECT 1',
        'ALTER TABLE task_file ADD COLUMN unpack_dir VARCHAR(500) DEFAULT NULL COMMENT ''校验后真实执行目录，可等于 workdir 或其子目录'' AFTER storage_path'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- task_file.relative_path
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'task_db'
              AND TABLE_NAME = 'task_file'
              AND COLUMN_NAME = 'relative_path'
        ),
        'SELECT 1',
        'ALTER TABLE task_file ADD COLUMN relative_path VARCHAR(500) DEFAULT NULL COMMENT ''相对路径（解压后）'' AFTER unpack_dir'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- task_file.archive_flag
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'task_db'
              AND TABLE_NAME = 'task_file'
              AND COLUMN_NAME = 'archive_flag'
        ),
        'SELECT 1',
        'ALTER TABLE task_file ADD COLUMN archive_flag TINYINT NOT NULL DEFAULT 0 COMMENT ''是否归档包：1是，0否'' AFTER relative_path'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill archive marker from file_role
UPDATE task_file
SET archive_flag = CASE WHEN UPPER(file_role) = 'ARCHIVE' THEN 1 ELSE 0 END
WHERE archive_flag IS NULL OR archive_flag NOT IN (0, 1);

SET FOREIGN_KEY_CHECKS = 1;
