# ZIP_ONLY Regression Checklist

## 1. Purpose

This checklist verifies the new task input contract:

- upload mode is ZIP_ONLY
- only one archive per task is accepted
- archive metadata is persisted for later dispatch
- validation returns structured issues when invalid

## 2. Preconditions

- All services are running.
- Databases are initialized with latest schema or patched with docs/sql/patch_zip_only_schema_upgrade.sql.
- A valid user token is available.

## 3. Quick Variables

Use these values during manual run:

- GATEWAY_BASE_URL: http://localhost:8080
- USER_ID: from /api/auth/me
- PROFILE_ID: a profile whose uploadMode is ZIP_ONLY
- SOLVER_ID: solver linked to PROFILE_ID

## 4. Contract Checks

1. Query upload spec

- GET /api/profiles/{profileId}/upload-spec
- Expect:
  - data.uploadMode = ZIP_ONLY
  - data.archiveRule.fileKey = input_archive
  - data.archiveRule.allowSuffix contains zip

2. Create task

- POST /api/tasks
- Headers: X-User-Id
- Body includes solverId, profileId, taskType
- Expect code=0 and taskId returned

3. Upload valid archive

- POST /api/tasks/{taskId}/files (multipart)
- Params:
  - file: valid-case.zip
  - fileKey: input_archive
  - fileRole: ARCHIVE
- Headers: X-User-Id
- Expect code=0

4. Validate task success path

- POST /api/tasks/{taskId}/validate
- Headers: X-User-Id
- Expect:
  - code=0
  - data.valid = true
  - data.status = VALIDATED

5. Submit task

- POST /api/tasks/{taskId}/submit
- Headers: X-User-Id
- Expect code=0 and queued-like status

## 5. Negative Cases

1. Invalid suffix

- Upload file invalid-suffix.rar
- Expect upload reject or validate reject.
- If rejected in validate response, expect issues entry with:
  - errorCode = INVALID_ARCHIVE_SUFFIX

2. Missing archive

- Create task but skip upload.
- Validate.
- Expect issues contains:
  - errorCode = ARCHIVE_MISSING

3. Broken archive

- Upload a corrupted zip.
- Validate.
- Expect issues contains:
  - errorCode = ARCHIVE_BROKEN

4. Unsafe archive path

- Upload zip containing ../ in entry path.
- Validate.
- Expect issues contains:
  - errorCode = ARCHIVE_UNSAFE_PATH

5. Rule mismatch

- Upload archive missing required path/files.
- Validate.
- Expect one of:
  - MISSING_REQUIRED_PATH
  - INVALID_FILE_TYPE
  - INVALID_FILENAME_PATTERN
  - INVALID_SUFFIX
  - PATH_COUNT_OUT_OF_RANGE

## 6. Data Persistence Checks

After successful validation, query task files and verify:

- fileRole is ARCHIVE for input archive
- fileKey is input_archive
- archiveFlag = 1
- unpackDir is not empty

Related API:

- GET /api/tasks/{taskId}/files

## 7. Pass Criteria

All below must pass:

- ZIP_ONLY upload contract checks pass.
- At least one success path task reaches VALIDATED and submit succeeds.
- At least three negative cases return structured issues with expected errorCode.
- task_file metadata includes archiveFlag and unpackDir after successful validate.
