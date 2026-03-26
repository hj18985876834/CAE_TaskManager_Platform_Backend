-- CAE TaskManager platform database initialization script
-- Databases: user_db, solver_db, task_db, scheduler_db

CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS solver_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS task_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS scheduler_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =========================
-- user_db (2 tables)
-- =========================
USE user_db;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(30) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    role_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    KEY idx_role_id (role_id),
    KEY idx_status (status)
) ENGINE=InnoDB;

INSERT INTO sys_role (id, role_code, role_name)
VALUES (1, 'ADMIN', 'Administrator'), (2, 'USER', 'Standard User')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- Password for admin is 123456 (SHA-256)
INSERT INTO sys_user (id, username, password, real_name, role_id, status)
VALUES (1, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'System Admin', 1, 1)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), status = VALUES(status);

-- =========================
-- solver_db (3 tables)
-- =========================
USE solver_db;

CREATE TABLE IF NOT EXISTS solver_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    solver_code VARCHAR(50) NOT NULL,
    solver_name VARCHAR(100) NOT NULL,
    version VARCHAR(50) DEFAULT NULL,
    exec_mode VARCHAR(30) DEFAULT 'LOCAL',
    exec_path VARCHAR(255) DEFAULT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_solver_code (solver_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS solver_task_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    solver_id BIGINT NOT NULL,
    profile_code VARCHAR(50) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    profile_name VARCHAR(100) NOT NULL,
    command_template VARCHAR(255) DEFAULT NULL,
    parser_name VARCHAR(100) DEFAULT NULL,
    timeout_seconds INT DEFAULT 3600,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_profile_code (profile_code),
    KEY idx_solver_id (solver_id),
    KEY idx_task_type (task_type)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS solver_profile_file_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    file_key VARCHAR(50) NOT NULL,
    file_name_pattern VARCHAR(100) DEFAULT NULL,
    file_type VARCHAR(30) NOT NULL,
    required_flag TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    remark VARCHAR(255) DEFAULT NULL,
    KEY idx_profile_id (profile_id)
) ENGINE=InnoDB;

-- =========================
-- task_db (6 tables)
-- =========================
USE task_db;

CREATE TABLE IF NOT EXISTS sim_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(50) NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    solver_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    node_id BIGINT DEFAULT NULL,
    params_json TEXT DEFAULT NULL,
    submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    start_time DATETIME DEFAULT NULL,
    end_time DATETIME DEFAULT NULL,
    fail_type VARCHAR(50) DEFAULT NULL,
    fail_message VARCHAR(500) DEFAULT NULL,
    deleted_flag TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_solver_id (solver_id),
    KEY idx_profile_id (profile_id),
    KEY idx_submit_time (submit_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    from_status VARCHAR(30) DEFAULT NULL,
    to_status VARCHAR(30) NOT NULL,
    change_reason VARCHAR(255) DEFAULT NULL,
    operator_type VARCHAR(30) NOT NULL,
    operator_id BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task_id (task_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    file_role VARCHAR(30) NOT NULL,
    file_key VARCHAR(50) DEFAULT NULL,
    origin_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    file_suffix VARCHAR(20) DEFAULT NULL,
    checksum VARCHAR(100) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task_id (task_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_result_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    success_flag TINYINT NOT NULL,
    duration_seconds INT DEFAULT NULL,
    summary_text VARCHAR(500) DEFAULT NULL,
    metrics_json TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_task_id (task_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_result_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    file_type VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task_id (task_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_log_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    seq_no INT NOT NULL,
    log_content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_seq (task_id, seq_no),
    KEY idx_task_id_seq_no (task_id, seq_no)
) ENGINE=InnoDB;

-- =========================
-- scheduler_db (3 tables)
-- =========================
USE scheduler_db;

CREATE TABLE IF NOT EXISTS compute_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    node_code VARCHAR(50) NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    host VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    max_concurrency INT NOT NULL DEFAULT 1,
    running_count INT NOT NULL DEFAULT 0,
    cpu_usage DECIMAL(5,2) DEFAULT 0,
    memory_usage DECIMAL(5,2) DEFAULT 0,
    last_heartbeat_time DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_node_code (node_code),
    KEY idx_status (status),
    KEY idx_last_heartbeat_time (last_heartbeat_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS node_solver_capability (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    node_id BIGINT NOT NULL,
    solver_id BIGINT NOT NULL,
    solver_version VARCHAR(50) DEFAULT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_node_id (node_id),
    KEY idx_solver_id (solver_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS schedule_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    node_id BIGINT NOT NULL,
    strategy_name VARCHAR(50) NOT NULL,
    schedule_status VARCHAR(30) NOT NULL,
    schedule_message VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task_id (task_id),
    KEY idx_node_id (node_id)
) ENGINE=InnoDB;
