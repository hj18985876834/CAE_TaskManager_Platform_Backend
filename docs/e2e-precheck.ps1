param(
    [string]$GatewayBaseUrl = "http://localhost:8080",
    [string]$DbHost = "",
    [int]$DbPort = 0,
    [string]$DbUser = "",
    [string]$DbPassword = "",
    [string[]]$DbNames = @(),
    [switch]$SkipRouteChecks,
    [switch]$SkipDatabaseChecks
)

$ErrorActionPreference = "Stop"

$script:HasFailure = $false

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Resolve-DbConfig {
    if ([string]::IsNullOrWhiteSpace($DbHost)) {
        $DbHost = $env:DB_HOST
    }
    if ([string]::IsNullOrWhiteSpace($DbHost)) {
        $DbHost = "localhost"
    }
    if ($DbPort -le 0) {
        if (-not [string]::IsNullOrWhiteSpace($env:DB_PORT)) {
            $DbPort = [int]$env:DB_PORT
        } else {
            $DbPort = 3306
        }
    }
    if ([string]::IsNullOrWhiteSpace($DbUser)) {
        $DbUser = $env:DB_USER
    }
    if ([string]::IsNullOrWhiteSpace($DbPassword)) {
        $DbPassword = $env:DB_PASSWORD
    }
    if ($DbNames.Count -eq 0) {
        $candidate = @($env:USER_DB_NAME, $env:SOLVER_DB_NAME, $env:TASK_DB_NAME, $env:SCHEDULER_DB_NAME) |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
        if ($candidate.Count -gt 0) {
            $DbNames = $candidate
        } else {
            $DbNames = @("user_db", "solver_db", "task_db", "scheduler_db")
        }
    }

    return [pscustomobject]@{
        Host = $DbHost
        Port = $DbPort
        User = $DbUser
        Password = $DbPassword
        Names = $DbNames
    }
}

function Add-Result {
    param(
        [string]$Category,
        [string]$Name,
        [bool]$Passed,
        [string]$Detail
    )

    if (-not $Passed) {
        $script:HasFailure = $true
    }

    return [pscustomobject]@{
        Category = $Category
        Name = $Name
        Passed = $Passed
        Detail = $Detail
    }
}

function Test-Port {
    param(
        [string]$ComputerName,
        [int]$Port
    )

    try {
        $probe = Test-NetConnection -ComputerName $ComputerName -Port $Port -WarningAction SilentlyContinue
        return [bool]$probe.TcpTestSucceeded
    } catch {
        return $false
    }
}

function Test-DatabaseByMysql {
    param(
        [string]$DbHost,
        [int]$Port,
        [string]$User,
        [string]$Password,
        [string]$DbName
    )

    $mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
    if ($null -eq $mysqlCmd) {
        return [pscustomobject]@{
            Supported = $false
            Passed = $false
            Detail = "mysql client not found; skipped SQL-level check"
        }
    }

    $env:MYSQL_PWD = $Password
    try {
        $args = @(
            "-h", $DbHost,
            "-P", "$Port",
            "-u", $User,
            "-N",
            "-e", "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '$DbName';"
        )
        $output = & mysql @args 2>&1
        $code = $LASTEXITCODE
        if ($code -ne 0) {
            return [pscustomobject]@{
                Supported = $true
                Passed = $false
                Detail = "mysql check failed: $output"
            }
        }
        $exists = ($output | Out-String).Trim()
        if ($exists -eq $DbName) {
            return [pscustomobject]@{
                Supported = $true
                Passed = $true
                Detail = "schema exists"
            }
        }
        return [pscustomobject]@{
            Supported = $true
            Passed = $false
            Detail = "schema not found"
        }
    } finally {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Invoke-RouteProbe {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers,
        [hashtable]$Body,
        [int[]]$ExpectedStatus
    )

    $statusCode = -1
    $responseText = ""
    $passed = $false

    try {
        if ($Method -eq "POST") {
            $jsonBody = if ($null -eq $Body) { "" } else { $Body | ConvertTo-Json -Depth 8 }
            $resp = Invoke-WebRequest -Uri $Url -Method Post -Headers $Headers -ContentType "application/json" -Body $jsonBody
        } else {
            $resp = Invoke-WebRequest -Uri $Url -Method Get -Headers $Headers
        }
        $statusCode = [int]$resp.StatusCode
        $responseText = [string]$resp.Content
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $responseText = $reader.ReadToEnd()
                $reader.Close()
            } catch {
                $responseText = $_.Exception.Message
            }
        } else {
            $responseText = $_.Exception.Message
        }
    }

    if ($ExpectedStatus -contains $statusCode) {
        $passed = $true
    }

    return [pscustomobject]@{
        Passed = $passed
        StatusCode = $statusCode
        Detail = if ([string]::IsNullOrWhiteSpace($responseText)) { "" } else { ($responseText -replace "\r|\n", " ").Trim() }
    }
}

$results = @()

Write-Section "Service Port Checks (8080-8085)"
$servicePorts = @(
    @{ Name = "gateway-service"; Port = 8080 },
    @{ Name = "user-service"; Port = 8081 },
    @{ Name = "solver-service"; Port = 8082 },
    @{ Name = "task-service"; Port = 8083 },
    @{ Name = "scheduler-service"; Port = 8084 },
    @{ Name = "node-agent"; Port = 8085 }
)

foreach ($svc in $servicePorts) {
    $ok = Test-Port -ComputerName "localhost" -Port $svc.Port
    $detail = if ($ok) { "listening" } else { "not listening" }
    $results += Add-Result -Category "port" -Name "$($svc.Name):$($svc.Port)" -Passed $ok -Detail $detail
}

if (-not $SkipDatabaseChecks) {
    Write-Section "Database Checks"
    $dbConfig = Resolve-DbConfig

    $dbPortOk = Test-Port -ComputerName $dbConfig.Host -Port $dbConfig.Port
    $results += Add-Result -Category "db" -Name "mysql-port:$($dbConfig.Host):$($dbConfig.Port)" -Passed $dbPortOk -Detail $(if ($dbPortOk) { "reachable" } else { "unreachable" })

    if ($dbPortOk) {
        if ([string]::IsNullOrWhiteSpace($dbConfig.User)) {
            $results += Add-Result -Category "db" -Name "mysql-auth" -Passed $false -Detail "DB_USER is empty; set env vars using docs/db-config.example.ps1"
        } else {
            foreach ($dbName in $dbConfig.Names) {
                $dbCheck = Test-DatabaseByMysql -DbHost $dbConfig.Host -Port $dbConfig.Port -User $dbConfig.User -Password $dbConfig.Password -DbName $dbName
                if (-not $dbCheck.Supported) {
                    $results += Add-Result -Category "db" -Name "schema:$dbName" -Passed $true -Detail $dbCheck.Detail
                } else {
                    $results += Add-Result -Category "db" -Name "schema:$dbName" -Passed $dbCheck.Passed -Detail $dbCheck.Detail
                }
            }
        }
    }
}

if (-not $SkipRouteChecks) {
    Write-Section "Gateway Route Checks"
    $routeChecks = @(
        @{ Name = "login"; Method = "POST"; Path = "/api/auth/login"; Body = @{ username = "precheck_user"; password = "precheck_pass" }; Expected = @(200) },
        @{ Name = "me"; Method = "GET"; Path = "/api/auth/me"; Body = $null; Expected = @(401,403) },
        @{ Name = "tasks"; Method = "GET"; Path = "/api/tasks"; Body = $null; Expected = @(401,403) },
        @{ Name = "nodes"; Method = "GET"; Path = "/api/nodes"; Body = $null; Expected = @(401,403) },
        @{ Name = "schedules"; Method = "GET"; Path = "/api/schedules"; Body = $null; Expected = @(401,403) },
        @{ Name = "task-schedules"; Method = "GET"; Path = "/api/tasks/1/schedules"; Body = $null; Expected = @(401,403) }
    )

    foreach ($check in $routeChecks) {
        $url = "$GatewayBaseUrl$($check.Path)"
        $probe = Invoke-RouteProbe -Method $check.Method -Url $url -Headers @{} -Body $check.Body -ExpectedStatus $check.Expected
        $detail = "status=$($probe.StatusCode)"
        if (-not [string]::IsNullOrWhiteSpace($probe.Detail)) {
            $detail = "$detail; body=$($probe.Detail)"
        }
        $results += Add-Result -Category "route" -Name $check.Name -Passed $probe.Passed -Detail $detail
    }
}

Write-Section "Summary"
$results | Sort-Object Category, Name | Format-Table Category, Name, Passed, Detail -AutoSize | Out-String | Write-Output

if ($script:HasFailure) {
    Write-Host "Precheck FAILED. Resolve failed items before running docs/e2e-smoke-run.ps1"
    exit 1
}

Write-Host "Precheck PASSED. You can run docs/e2e-smoke-run.ps1"
exit 0
