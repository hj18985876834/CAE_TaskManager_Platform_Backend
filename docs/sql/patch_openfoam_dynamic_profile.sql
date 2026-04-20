-- =========================================================
-- OpenFOAM dynamic application profile patch
-- Target: MySQL 8.0+
-- Scope: solver_db
-- Note: rerunnable
-- =========================================================

SET NAMES utf8mb4;

USE solver_db;

SET @openfoam_profile_id = (
    SELECT id
    FROM solver_task_profile
    WHERE profile_code = 'CFD_STEADY_DEFAULT'
    LIMIT 1
);

UPDATE solver_task_profile
SET command_template = '${openfoamApplication} -case ${taskDir}'
WHERE id = @openfoam_profile_id;

DELETE FROM solver_profile_file_rule
WHERE profile_id = @openfoam_profile_id
  AND file_key IN (
      'system_control_dict',
      'system_fv_schemes',
      'system_fv_solution',
      'of_incompressible_u',
      'of_incompressible_p',
      'of_incompressible_transport',
      'of_compressible_u',
      'of_compressible_p',
      'of_compressible_thermo',
      'of_interfoam_u',
      'of_interfoam_p_rgh',
      'of_interfoam_alpha',
      'of_interfoam_transport',
      'of_interfoam_g',
      'of_laplacian_t',
      'of_laplacian_transport'
  );

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'system_control_dict', 'system/controlDict', 'controlDict', 'FILE', 1, 2, '{"solverCodes":["OPENFOAM"],"deriveParam":{"name":"openfoamApplication","source":"fileContentRegex","pattern":"(?m)^\\\\s*application\\\\s+([^;]+?)\\\\s*;","group":1,"stripQuotes":true,"sanitizeRegex":"^[A-Za-z][A-Za-z0-9_+-]*$","required":true,"preprocess":[{"pattern":"(?s)/\\\\*.*?\\\\*/","replacement":" "},{"pattern":"(?m)//.*$","replacement":" "}]}}', 'OpenFOAM controlDict'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'system_fv_schemes', 'system/fvSchemes', 'fvSchemes', 'FILE', 1, 3, '{"solverCodes":["OPENFOAM"]}', 'OpenFOAM fvSchemes'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'system_fv_solution', 'system/fvSolution', 'fvSolution', 'FILE', 1, 4, '{"solverCodes":["OPENFOAM"]}', 'OpenFOAM fvSolution'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_incompressible_u', '0/U', 'U', 'FILE', 1, 10, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["simpleFoam","icoFoam","pimpleFoam","pisoFoam"]}', 'OpenFOAM incompressible velocity field'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_incompressible_p', '0/p', 'p', 'FILE', 1, 11, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["simpleFoam","icoFoam","pimpleFoam","pisoFoam"]}', 'OpenFOAM incompressible pressure field'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_incompressible_transport', 'constant/transportProperties', 'transportProperties', 'FILE', 1, 12, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["simpleFoam","icoFoam","pimpleFoam","pisoFoam"]}', 'OpenFOAM incompressible transportProperties'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_compressible_u', '0/U', 'U', 'FILE', 1, 20, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["rhoSimpleFoam","rhoPimpleFoam","rhoPisoFoam","sonicFoam"]}', 'OpenFOAM compressible velocity field'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_compressible_p', '0/p', 'p', 'FILE', 1, 21, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["rhoSimpleFoam","rhoPimpleFoam","rhoPisoFoam","sonicFoam"]}', 'OpenFOAM compressible pressure field'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_compressible_thermo', 'constant/thermophysicalProperties', 'thermophysicalProperties', 'FILE', 1, 22, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["rhoSimpleFoam","rhoPimpleFoam","rhoPisoFoam","sonicFoam"]}', 'OpenFOAM thermophysicalProperties'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_interfoam_u', '0/U', 'U', 'FILE', 1, 30, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["interFoam"]}', 'interFoam velocity field'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_interfoam_p_rgh', '0/p_rgh', 'p_rgh', 'FILE', 1, 31, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["interFoam"]}', 'interFoam p_rgh field'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_interfoam_alpha', '0/alpha.*', 'alpha.*', 'FILE', 1, 32, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["interFoam"],"minCount":1}', 'interFoam alpha field'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_interfoam_transport', 'constant/transportProperties', 'transportProperties', 'FILE', 1, 33, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["interFoam"]}', 'interFoam transportProperties'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_interfoam_g', 'constant/g', 'g', 'FILE', 1, 34, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["interFoam"]}', 'interFoam gravity file'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_laplacian_t', '0/T', 'T', 'FILE', 1, 40, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["laplacianFoam"]}', 'laplacianFoam scalar field'
WHERE @openfoam_profile_id IS NOT NULL;

INSERT INTO solver_profile_file_rule
    (profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description)
SELECT @openfoam_profile_id, 'of_laplacian_transport', 'constant/transportProperties', 'transportProperties', 'FILE', 1, 41, '{"solverCodes":["OPENFOAM"],"openfoamApplications":["laplacianFoam"]}', 'laplacianFoam transportProperties'
WHERE @openfoam_profile_id IS NOT NULL;
