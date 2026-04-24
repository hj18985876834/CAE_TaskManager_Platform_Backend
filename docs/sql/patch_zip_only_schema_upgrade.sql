-- =========================================================
-- ZIP_ONLY schema upgrade patch
-- Target: MySQL 8.0+
-- Scope: solver_db, task_db, scheduler_db
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


-- Keep latest task_file metadata before adding idempotency key.
DELETE old_file
FROM task_file old_file
JOIN task_file new_file
  ON old_file.task_id = new_file.task_id
 AND old_file.file_role = new_file.file_role
 AND old_file.file_key = new_file.file_key
 AND old_file.id < new_file.id;

-- task_file idempotency key: one effective file per task + role + key.
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

-- Keep latest task_result_file metadata before adding idempotency key.
DELETE old_file
FROM task_result_file old_file
JOIN task_result_file new_file
  ON old_file.task_id = new_file.task_id
 AND old_file.file_type = new_file.file_type
 AND old_file.file_name = new_file.file_name
 AND old_file.id < new_file.id;

-- task_result_file idempotency key: repeated node reports update same logical result file.
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

-- ---------------------------------------------------------
-- 3) scheduler_db upgrades
-- ---------------------------------------------------------
USE scheduler_db;

-- compute_node.reserved_count
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'scheduler_db'
              AND TABLE_NAME = 'compute_node'
              AND COLUMN_NAME = 'reserved_count'
        ),
        'SELECT 1',
        'ALTER TABLE compute_node ADD COLUMN reserved_count INT NOT NULL DEFAULT 0 COMMENT ''调度预占任务数'' AFTER running_count'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- node_reservation: idempotency truth for node capacity reservation.
CREATE TABLE IF NOT EXISTS node_reservation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    node_id BIGINT NOT NULL COMMENT '节点ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    status VARCHAR(20) NOT NULL COMMENT 'RESERVED / RELEASED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    released_at DATETIME DEFAULT NULL COMMENT '释放时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_node_task (node_id, task_id),
    KEY idx_node_status (node_id, status),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点预占记录表';

SET FOREIGN_KEY_CHECKS = 1;
