# Run this in PowerShell before starting services
# Update values to your local MySQL account and schema names

$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_USER = "root"
$env:DB_PASSWORD = "111111"

$env:USER_DB_NAME = "user_db"
$env:SOLVER_DB_NAME = "solver_db"
$env:TASK_DB_NAME = "task_db"
$env:SCHEDULER_DB_NAME = "scheduler_db"

Write-Host "Database environment variables have been set for current shell."
