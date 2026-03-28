-- =========================================================
-- 分布式仿真任务调度系统 - 数据库初始化脚本
-- MySQL 8.0+
-- 字符集：utf8mb4
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 1. user_db
-- =========================================================
DROP DATABASE IF EXISTS user_db;
CREATE DATABASE user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE user_db;

-- -----------------------------
-- 表 1：sys_role
-- -----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '主键ID',
    role_code VARCHAR(30) NOT NULL COMMENT '角色编码，如 ADMIN / USER',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色定义表';

-- -----------------------------
-- 表 2：sys_user
-- -----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名，唯一',
    password VARCHAR(255) NOT NULL COMMENT '加密密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role_id (role_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始化角色数据
INSERT INTO sys_role (id, role_code, role_name, created_at) VALUES
(1, 'ADMIN', '管理员', NOW()),
(2, 'USER', '普通用户', NOW());

-- =========================================================
-- 2. solver_db
-- =========================================================
DROP DATABASE IF EXISTS solver_db;
CREATE DATABASE solver_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE solver_db;

-- -----------------------------
-- 表 3：solver_definition
-- -----------------------------
DROP TABLE IF EXISTS solver_definition;
CREATE TABLE solver_definition (
    id BIGINT NOT NULL COMMENT '主键ID',
    solver_code VARCHAR(50) NOT NULL COMMENT '唯一编码，如 OPENFOAM / CALCULIX / MOCK',
    solver_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    version VARCHAR(50) DEFAULT NULL COMMENT '版本',
    exec_mode VARCHAR(30) NOT NULL COMMENT '执行方式：LOCAL / CONTAINER',
    exec_path VARCHAR(255) DEFAULT NULL COMMENT '可执行路径或镜像名',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(255) DEFAULT NULL COMMENT '说明/描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_solver_code (solver_code),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求解器定义表';

-- -----------------------------
-- 表 4：solver_task_profile
-- -----------------------------
DROP TABLE IF EXISTS solver_task_profile;
CREATE TABLE solver_task_profile (
    id BIGINT NOT NULL COMMENT '主键ID',
    solver_id BIGINT NOT NULL COMMENT '求解器ID',
    profile_code VARCHAR(50) NOT NULL COMMENT '模板编码',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型，如 STRUCT_STATIC / CFD_STEADY',
    profile_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    command_template VARCHAR(255) NOT NULL COMMENT '命令模板',
    params_schema_json TEXT DEFAULT NULL COMMENT '参数模板定义JSON',
    parser_name VARCHAR(100) DEFAULT NULL COMMENT '结果解析器名称',
    timeout_seconds INT NOT NULL DEFAULT 3600 COMMENT '超时时间（秒）',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(255) DEFAULT NULL COMMENT '说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_profile_code (profile_code),
    KEY idx_solver_id (solver_id),
    KEY idx_task_type (task_type),
    KEY idx_enabled (enabled),
    KEY idx_solver_task_type (solver_id, task_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求解器任务模板表';

-- -----------------------------
-- 表 5：solver_profile_file_rule
-- -----------------------------
DROP TABLE IF EXISTS solver_profile_file_rule;
CREATE TABLE solver_profile_file_rule (
    id BIGINT NOT NULL COMMENT '主键ID',
    profile_id BIGINT NOT NULL COMMENT '模板ID',
    file_key VARCHAR(50) NOT NULL COMMENT '文件标识，如 main_inp / case_zip',
    file_name_pattern VARCHAR(100) DEFAULT NULL COMMENT '文件名或匹配规则',
    file_type VARCHAR(30) NOT NULL COMMENT 'FILE / DIR / ZIP',
    required_flag TINYINT NOT NULL DEFAULT 1 COMMENT '是否必需',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    description VARCHAR(255) DEFAULT NULL COMMENT '说明',
    PRIMARY KEY (id),
    KEY idx_profile_id (profile_id),
    KEY idx_file_key (file_key),
    KEY idx_profile_sort (profile_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板文件规则表';

-- =========================================================
-- 3. task_db
-- =========================================================
DROP DATABASE IF EXISTS task_db;
CREATE DATABASE task_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE task_db;

-- -----------------------------
-- 表 6：sim_task
-- -----------------------------
DROP TABLE IF EXISTS sim_task;
CREATE TABLE sim_task (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_no VARCHAR(50) NOT NULL COMMENT '任务编号，唯一',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    user_id BIGINT NOT NULL COMMENT '提交用户ID',
    solver_id BIGINT NOT NULL COMMENT '求解器ID',
    profile_id BIGINT NOT NULL COMMENT '求解器任务模板ID',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
    status VARCHAR(30) NOT NULL COMMENT '当前状态',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级，默认0',
    node_id BIGINT DEFAULT NULL COMMENT '分配节点ID，可为空',
    params_json TEXT DEFAULT NULL COMMENT '参数JSON',
    submit_time DATETIME DEFAULT NULL COMMENT '提交时间',
    start_time DATETIME DEFAULT NULL COMMENT '开始执行时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    fail_type VARCHAR(50) DEFAULT NULL COMMENT '失败类型',
    fail_message VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    deleted_flag TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_solver_id (solver_id),
    KEY idx_profile_id (profile_id),
    KEY idx_submit_time (submit_time),
    KEY idx_node_id (node_id),
    KEY idx_deleted_flag (deleted_flag),
    KEY idx_status_priority_created (status, priority, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仿真任务主表';

-- -----------------------------
-- 表 7：task_status_history
-- -----------------------------
DROP TABLE IF EXISTS task_status_history;
CREATE TABLE task_status_history (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    from_status VARCHAR(30) DEFAULT NULL COMMENT '原状态',
    to_status VARCHAR(30) NOT NULL COMMENT '新状态',
    change_reason VARCHAR(255) DEFAULT NULL COMMENT '变更原因',
    operator_type VARCHAR(30) NOT NULL COMMENT '操作来源：SYSTEM / USER / NODE / ADMIN',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID，可为空',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_created_at (created_at),
    KEY idx_task_created_at (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务状态流转记录表';

-- -----------------------------
-- 表 8：task_file
-- -----------------------------
DROP TABLE IF EXISTS task_file;
CREATE TABLE task_file (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    file_role VARCHAR(30) NOT NULL COMMENT '文件角色：INPUT / CONFIG / ARCHIVE',
    file_key VARCHAR(50) NOT NULL COMMENT '对应模板中的文件标识',
    origin_name VARCHAR(255) NOT NULL COMMENT '原文件名',
    storage_path VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
    file_suffix VARCHAR(20) DEFAULT NULL COMMENT '后缀名',
    checksum VARCHAR(100) DEFAULT NULL COMMENT '校验值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_file_key (file_key),
    KEY idx_task_file_role (task_id, file_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务输入文件表';

-- -----------------------------
-- 表 9：task_result_summary
-- -----------------------------
DROP TABLE IF EXISTS task_result_summary;
CREATE TABLE task_result_summary (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    success_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否成功',
    duration_seconds INT DEFAULT NULL COMMENT '执行耗时（秒）',
    summary_text VARCHAR(500) DEFAULT NULL COMMENT '结果摘要',
    metrics_json TEXT DEFAULT NULL COMMENT '指标JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务结果摘要表';

-- -----------------------------
-- 表 10：task_result_file
-- -----------------------------
DROP TABLE IF EXISTS task_result_file;
CREATE TABLE task_result_file (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    file_type VARCHAR(30) NOT NULL COMMENT 'RESULT / LOG / REPORT / IMAGE',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    storage_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_file_type (file_type),
    KEY idx_task_file_type (task_id, file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务结果文件表';

-- -----------------------------
-- 表 11：task_log_chunk
-- -----------------------------
DROP TABLE IF EXISTS task_log_chunk;
CREATE TABLE task_log_chunk (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    seq_no INT NOT NULL COMMENT '分片序号',
    log_content TEXT COMMENT '日志内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '产生时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_seq (task_id, seq_no),
    KEY idx_task_id_seq_no (task_id, seq_no),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务日志分片表';

-- =========================================================
-- 4. scheduler_db
-- =========================================================
DROP DATABASE IF EXISTS scheduler_db;
CREATE DATABASE scheduler_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE scheduler_db;

-- -----------------------------
-- 表 12：compute_node
-- -----------------------------
DROP TABLE IF EXISTS compute_node;
CREATE TABLE compute_node (
    id BIGINT NOT NULL COMMENT '主键ID',
    node_code VARCHAR(50) NOT NULL COMMENT '节点编码，唯一',
    node_name VARCHAR(100) NOT NULL COMMENT '节点名称',
    node_token VARCHAR(255) NOT NULL COMMENT '节点凭证',
    host VARCHAR(100) NOT NULL COMMENT 'IP或主机名',
    status VARCHAR(20) NOT NULL COMMENT 'ONLINE / OFFLINE / DISABLED',
    max_concurrency INT NOT NULL DEFAULT 1 COMMENT '最大并发任务数',
    running_count INT NOT NULL DEFAULT 0 COMMENT '当前运行任务数',
    cpu_usage DECIMAL(5,2) DEFAULT 0.00 COMMENT 'CPU占用率',
    memory_usage DECIMAL(5,2) DEFAULT 0.00 COMMENT '内存占用率',
    last_heartbeat_time DATETIME DEFAULT NULL COMMENT '最近心跳时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_node_code (node_code),
    UNIQUE KEY uk_node_token (node_token),
    KEY idx_status (status),
    KEY idx_last_heartbeat_time (last_heartbeat_time),
    KEY idx_status_running_count (status, running_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计算节点表';

-- -----------------------------
-- 表 13：node_solver_capability
-- -----------------------------
DROP TABLE IF EXISTS node_solver_capability;
CREATE TABLE node_solver_capability (
    id BIGINT NOT NULL COMMENT '主键ID',
    node_id BIGINT NOT NULL COMMENT '节点ID',
    solver_id BIGINT NOT NULL COMMENT '求解器ID',
    solver_version VARCHAR(50) DEFAULT NULL COMMENT '节点安装版本',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否可用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_node_solver (node_id, solver_id),
    KEY idx_node_id (node_id),
    KEY idx_solver_id (solver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点求解器能力表';

-- -----------------------------
-- 表 14：schedule_record
-- -----------------------------
DROP TABLE IF EXISTS schedule_record;
CREATE TABLE schedule_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    node_id BIGINT NOT NULL COMMENT '目标节点ID',
    strategy_name VARCHAR(50) NOT NULL COMMENT '调度策略名',
    schedule_status VARCHAR(30) NOT NULL COMMENT 'SUCCESS / FAILED',
    schedule_message VARCHAR(255) DEFAULT NULL COMMENT '调度说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调度时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_node_id (node_id),
    KEY idx_created_at (created_at),
    KEY idx_task_node (task_id, node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度记录表';

SET FOREIGN_KEY_CHECKS = 1;