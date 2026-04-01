# Docker Compose Quick Start

## Goal

Provide a runnable local container topology for:

- mysql
- gateway-service
- user-service
- solver-service
- task-service
- scheduler-service
- node-agent

## Files

- compose file: [compose.yaml](/d:/Project/CAE_TaskManager_Platform/Backend/compose.yaml)
- env template: [docker-compose.example.env](/d:/Project/CAE_TaskManager_Platform/Backend/docs/env/docker-compose.example.env)
- image build template: [Dockerfile.service](/d:/Project/CAE_TaskManager_Platform/Backend/Dockerfile.service)

## Start

1. Copy `docs/env/docker-compose.example.env` to a local env file if you want to customize values.
2. Run:

```powershell
docker compose --env-file docs/env/docker-compose.example.env up --build
```

3. Gateway default URL:

```text
http://localhost:8080
```

## Important Notes

- Current implementation still depends on shared local filesystem paths between `task-service` and `node-agent`.
- `task-service` task root is now configurable through `TASK_STORAGE_ROOT`, defaulting to `./data/tasks` locally and `/app/data/tasks` in compose.
- `task-service` result file root is now configurable through `TASK_RESULT_ROOT`, and the default is aligned with the design docs under the same task root path.
- `node-agent` work root remains configurable through `NODE_WORK_ROOT`, and its default is also aligned to the same task root path.
- `compose.yaml` mounts one shared task-data volume to both services so uploaded inputs, logs, and result files stay under the document-aligned `/data/tasks` style structure.
- `init_databases.sql` now uses configurable demo node hosts. For compose, the default `NODE_ADVERTISED_HOST=node-agent:8085` is the expected setting.

## Current Limitations

- Solver execution is still local-process style inside `node-agent`; real solver binaries are not bundled in the images.
- If OpenFOAM or CalculiX binaries are required, extend the `node-agent` image or mount them from the host.
- Health checks are only defined for MySQL. Service startup ordering relies on database readiness plus Spring Boot retry behavior.
