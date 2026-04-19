# Run this in PowerShell before starting services
# Update values to your local MySQL account and service endpoints

$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_USERNAME = "root"
$env:DB_USER = "root"
$env:DB_PASSWORD = "111111"

$env:GATEWAY_BASE_URL = "http://localhost:8080"
$env:USER_SERVICE_BASE_URL = "http://localhost:8081"
$env:SOLVER_SERVICE_BASE_URL = "http://localhost:8082"
$env:TASK_SERVICE_BASE_URL = "http://localhost:8083"
$env:SCHEDULER_SERVICE_BASE_URL = "http://localhost:8084"
$env:NODE_AGENT_BASE_URL = "http://localhost:8085"
$taskRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\\data\\tasks"))
$env:TASK_STORAGE_ROOT = $taskRoot
$env:TASK_RESULT_ROOT = $taskRoot
$env:NODE_ADVERTISED_HOST = "127.0.0.1:8085"
$env:NODE_WORK_ROOT = $taskRoot

$env:USER_DB_NAME = "user_db"
$env:SOLVER_DB_NAME = "solver_db"
$env:TASK_DB_NAME = "task_db"
$env:SCHEDULER_DB_NAME = "scheduler_db"

Write-Host "Database and service environment variables have been set for current shell."
Write-Host "Resolved TASK_STORAGE_ROOT: $taskRoot"
