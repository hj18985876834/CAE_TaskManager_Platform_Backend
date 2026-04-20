-- =========================================================
-- CalculiX command template alignment patch
-- Target: MySQL 8.0+
-- Scope: solver_db
-- Note: rerunnable
-- =========================================================

SET NAMES utf8mb4;

USE solver_db;

UPDATE solver_task_profile
SET command_template = '${solverExecPath} ${taskDir}/model'
WHERE profile_code = 'STRUCT_STATIC_DEFAULT'
  AND solver_id = (
      SELECT id
      FROM solver_definition
      WHERE solver_code = 'CALCULIX'
      LIMIT 1
  );
