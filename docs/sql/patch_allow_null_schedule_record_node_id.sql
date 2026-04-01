-- =========================================================
-- Patch: allow schedule_record.node_id to be NULL
-- Reason:
--   Scheduling failures such as "no available node" are recorded
--   before a target node is selected, so node_id must be nullable.
-- Target database:
--   scheduler_db
-- =========================================================

USE scheduler_db;

ALTER TABLE schedule_record
    MODIFY COLUMN node_id BIGINT DEFAULT NULL COMMENT '目标节点ID，可为空（如无可用节点时）';
