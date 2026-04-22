# CAE TaskManager E2E Smoke Checklist

## 1. Goal

Verify one full business chain across gateway, user-service, task-service, scheduler-service, and node-agent:

- login
- create task
- upload files
- validate
- submit
- scheduled and dispatched
- node-agent execution and reports
- query final task/result/log

Also verify the ZIP_ONLY contract introduced for task input:

- upload mode is ZIP_ONLY
- archive upload key is input_archive
- upload role is ARCHIVE
- validation returns structured issues[] when invalid

## 2. Service Matrix

These are default local ports. In Docker Compose or other environments, prefer configuring base URLs through environment variables instead of assuming localhost.

- `GATEWAY_BASE_URL`
- `USER_SERVICE_BASE_URL`
- `SOLVER_SERVICE_BASE_URL`
- `TASK_SERVICE_BASE_URL`
- `SCHEDULER_SERVICE_BASE_URL`
- `NODE_AGENT_BASE_URL`
- `NODE_ADVERTISED_HOST`

- gateway-service: 8080
- user-service: 8081
- solver-service: 8082
- task-service: 8083
- scheduler-service: 8084
- node-agent: 8085

## 3. Precheck (must pass before E2E)

### 3.1 Build

Run once from repository root:

- mvn -pl common-lib,gateway-service,user-service,solver-service,task-service,scheduler-service,node-agent -am compile

Expected:

- BUILD SUCCESS

### 3.2 Runtime Precheck

- MySQL databases are ready and seeded.
- All services are started.
- Gateway can access all downstream services.

Recommended command (repository docs folder):

- `./e2e-precheck.ps1`

Optional with explicit DB/env inputs:

- `./e2e-precheck.ps1 -GatewayBaseUrl "http://localhost:8080" -DbHost "localhost" -DbPort 3306`

Optional with explicit service URLs:

- `./e2e-precheck.ps1 -GatewayBaseUrl "http://gateway-service:8080" -TaskServiceBaseUrl "http://task-service:8083" -SchedulerServiceBaseUrl "http://scheduler-service:8084" -NodeAgentBaseUrl "http://node-agent:8085"`

ZIP_ONLY one-command smoke run:

- `./e2e-zip-only-smoke.ps1 -GatewayBaseUrl "http://localhost:8080" -Username "demo" -Password "123456" -SolverId 1 -ProfileId 1 -TaskType "SIMULATION"`

### 3.3 Route Consistency Gate

Current scheduler public paths are:

- /api/nodes
- /api/schedules

Task-owned schedule timeline path is exposed by task-service:

- /api/tasks/{taskId}/schedules

Gateway route config should at least forward:

- /api/nodes/**
- /api/schedules/**
- /api/tasks/**

Result:

- Strict gateway-only E2E can proceed.

## 4. Test Data

Use one normal user account and one admin account.

Suggested placeholders:

- userA: normal role
- adminA: admin role

ZIP_ONLY smoke files:

- valid-case.zip (contains required structure)
- invalid-suffix.rar (suffix check)
- unsafe-path.zip (contains ../ entry)

## 5. Main Flow Test Cases

## TC-01 Login and Token

Request:

- POST /api/auth/login (through gateway)

Expected:

- code=0
- response contains token

Output to capture:

- access_token

## TC-02 Current User

Request:

- GET /api/auth/me (through gateway)
- Authorization: Bearer <access_token>

Expected:

- code=0
- user id returned

Output to capture:

- user_id

## TC-03 Create Task

Request:

- POST /api/tasks (through gateway)
- Authorization: Bearer <access_token>

Expected:

- code=0
- taskId returned

Headers:

- X-User-Id required in direct task-service calls

Output to capture:

- task_id

## TC-04 Upload Task Files

Request:

- POST /api/tasks/{task_id}/files (through gateway, multipart)
- Authorization: Bearer <access_token>

Request constraints (ZIP_ONLY):

- fileKey should be input_archive
- fileRole should be ARCHIVE
- file suffix should be zip

Expected:

- code=0

Failure checks:

- non-zip should fail with 400 style response
- duplicate archive upload for one task should fail with conflict style response

## TC-05 Validate Task

Request:

- POST /api/tasks/{task_id}/validate (through gateway)
- Authorization: Bearer <access_token>

Expected:

- code=0
- task status becomes VALIDATED or equivalent

Failure checks (must include issues array in data):

- missing archive: ARCHIVE_MISSING
- broken archive: ARCHIVE_BROKEN
- unsafe path: ARCHIVE_UNSAFE_PATH
- missing required path: MISSING_REQUIRED_PATH

## TC-06 Submit Task

Request:

- POST /api/tasks/{task_id}/submit (through gateway)
- Authorization: Bearer <access_token>

Expected:

- code=0
- task enters QUEUED/PENDING for scheduler pickup

Negative check:

- submitting an unvalidated task should fail with TASK_NOT_VALIDATED style error

## TC-07 Scheduler Picks Task

Trigger:

- Wait one scheduler interval (default around 5s) or call scheduler internal flow manually if needed.

Expected:

- task-service receives mark-scheduled and mark-dispatched callbacks
- schedule record written as success or failure with reason

Verification API:

- GET /api/tasks/{task_id}
- GET /api/tasks/{task_id}/status-history

## TC-08 Node-Agent Executes and Reports

Security note:

- All node-agent callbacks except register must carry X-Node-Token.

Expected:

- task-service receives status-report/log-report/result-summary-report
- terminal state becomes FINISHED or FAILED with fail reason

Verification API:

- GET /api/tasks/{task_id}
- GET /api/tasks/{task_id}/logs
- GET /api/tasks/{task_id}/result-summary
- GET /api/tasks/{task_id}/result-files

## TC-09 Authorization Guard

Request:

- GET /api/tasks without token (through gateway)

Expected:

- 401 style failure

Request:

- GET /api/auth/login (whitelist) without token

Expected:

- not blocked by JWT filter

## TC-10 Traceability

Expected:

- same trace id can be found in gateway logs and downstream logs for one request chain

## 6. Failure Path Cases

- F-01 invalid token -> blocked at gateway
- F-02 node-agent dispatch fails -> scheduler records failure and task status is not silently lost
- F-03 result report partial failure -> failure reason can be queried from task history/log
- F-04 scheduler has no available node -> schedule failure record with explicit message
- F-05 archive suffix invalid -> validate result includes INVALID_ARCHIVE_SUFFIX
- F-06 archive unsafe path -> validate result includes ARCHIVE_UNSAFE_PATH
- F-07 required rule mismatch -> validate result includes MISSING_REQUIRED_PATH or INVALID_FILE_TYPE

## 7. Minimal Acceptance Criteria

All below must be true:

- A task can complete full chain from create to finished/failed with visible status history.
- Gateway JWT guard works for non-whitelist APIs.
- Scheduler and node-agent callbacks are observable in task-service data.
- Logs include trace id for troubleshooting.
- ZIP_ONLY invalid cases return structured issues[] with errorCode/message.

## 8. Execution Record Template

Use this template per run:

- Run ID:
- Branch/Commit:
- Environment:
- Start time:
- End time:

Case result table:

- TC-01: PASS/FAIL, notes
- TC-02: PASS/FAIL, notes
- TC-03: PASS/FAIL, notes
- TC-04: PASS/FAIL, notes
- TC-05: PASS/FAIL, notes
- TC-06: PASS/FAIL, notes
- TC-07: PASS/FAIL, notes
- TC-08: PASS/FAIL, notes
- TC-09: PASS/FAIL, notes
- TC-10: PASS/FAIL, notes

Blockers:

- blocker id, symptom, owner, target fix date

## 9. Next Automation Step

After manual smoke is stable for 3 runs:

- Convert TC-01 to TC-09 into a Newman or integration-test pipeline stage.
- Add it to CI as a required gate for merge to main.
