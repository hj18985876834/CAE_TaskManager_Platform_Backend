param(
    [string]$GatewayBaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "123456",
    [long]$SolverId = 1,
    [long]$ProfileId = 1,
    [string]$TaskType = "SIMULATION",
    [string]$TaskName = "smoke-task",
    [string]$UploadFilePath = "",
    [int]$ScheduleWaitSeconds = 12
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][hashtable]$Body,
        [hashtable]$Headers
    )
    $json = $Body | ConvertTo-Json -Depth 10
    return Invoke-RestMethod -Uri $Url -Method Post -ContentType "application/json" -Body $json -Headers $Headers
}

function Assert-Code0 {
    param(
        [Parameter(Mandatory = $true)]$Resp,
        [Parameter(Mandatory = $true)][string]$Step
    )
    if ($null -eq $Resp -or $Resp.code -ne 0) {
        throw "$Step failed: response code is not 0. Raw response: $($Resp | ConvertTo-Json -Depth 10)"
    }
}

Write-Host "[1/8] Login"
$loginResp = Invoke-JsonPost -Url "$GatewayBaseUrl/api/auth/login" -Body @{
    username = $Username
    password = $Password
}
Assert-Code0 -Resp $loginResp -Step "Login"
$token = $loginResp.data.token
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Login succeeded but token is empty."
}
$authHeader = @{ Authorization = "Bearer $token" }
Write-Host "Login OK, userId=$($loginResp.data.userId), role=$($loginResp.data.roleCode)"

Write-Host "[2/8] Query current user"
$meResp = Invoke-RestMethod -Uri "$GatewayBaseUrl/api/auth/me" -Method Get -Headers $authHeader
Assert-Code0 -Resp $meResp -Step "Query current user"
Write-Host "Current user OK"

Write-Host "[3/8] Create task"
$createResp = Invoke-JsonPost -Url "$GatewayBaseUrl/api/tasks" -Headers $authHeader -Body @{
    taskName = $TaskName
    solverId = $SolverId
    profileId = $ProfileId
    taskType = $TaskType
    params = @{}
}
Assert-Code0 -Resp $createResp -Step "Create task"
$taskId = $createResp.data.taskId
if ($null -eq $taskId) {
    throw "Create task succeeded but taskId is empty."
}
Write-Host "Task created: taskId=$taskId"

if (-not [string]::IsNullOrWhiteSpace($UploadFilePath)) {
    Write-Host "[4/8] Upload task files"
    if (-not (Test-Path -LiteralPath $UploadFilePath)) {
        throw "Upload file not found: $UploadFilePath"
    }
    $curlArgs = @(
        "-sS",
        "-X", "POST",
        "-H", "Authorization: Bearer $token",
        "-F", "files=@$UploadFilePath",
        "$GatewayBaseUrl/api/tasks/$taskId/files"
    )
    $uploadRaw = & curl.exe @curlArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Upload task files failed: curl exit code $LASTEXITCODE"
    }
    $uploadResp = $uploadRaw | ConvertFrom-Json
    Assert-Code0 -Resp $uploadResp -Step "Upload task files"
    Write-Host "Upload files OK"
} else {
    Write-Host "[4/8] Skip upload task files (UploadFilePath is empty)"
}

Write-Host "[5/8] Validate task"
$validateResp = Invoke-RestMethod -Uri "$GatewayBaseUrl/api/tasks/$taskId/validate" -Method Post -Headers $authHeader
Assert-Code0 -Resp $validateResp -Step "Validate task"
Write-Host "Validate task OK"

Write-Host "[6/8] Submit task"
$submitResp = Invoke-RestMethod -Uri "$GatewayBaseUrl/api/tasks/$taskId/submit" -Method Post -Headers $authHeader
Assert-Code0 -Resp $submitResp -Step "Submit task"
Write-Host "Submit task OK"

Write-Host "[7/8] Wait scheduler and node-agent flow"
Start-Sleep -Seconds $ScheduleWaitSeconds

Write-Host "[8/8] Query task detail, status history, logs, results"
$detailResp = Invoke-RestMethod -Uri "$GatewayBaseUrl/api/tasks/$taskId" -Method Get -Headers $authHeader
Assert-Code0 -Resp $detailResp -Step "Query task detail"

$historyResp = Invoke-RestMethod -Uri "$GatewayBaseUrl/api/tasks/$taskId/status-history" -Method Get -Headers $authHeader
Assert-Code0 -Resp $historyResp -Step "Query task status history"

$logResp = Invoke-RestMethod -Uri "$GatewayBaseUrl/api/tasks/$taskId/logs" -Method Get -Headers $authHeader
Assert-Code0 -Resp $logResp -Step "Query task logs"

$resultSummaryResp = Invoke-RestMethod -Uri "$GatewayBaseUrl/api/tasks/$taskId/result-summary" -Method Get -Headers $authHeader
Assert-Code0 -Resp $resultSummaryResp -Step "Query task result summary"

$resultFilesResp = Invoke-RestMethod -Uri "$GatewayBaseUrl/api/tasks/$taskId/result-files" -Method Get -Headers $authHeader
Assert-Code0 -Resp $resultFilesResp -Step "Query task result files"

Write-Host ""
Write-Host "E2E smoke run finished."
Write-Host "taskId=$taskId"
Write-Host "taskDetail=" ($detailResp.data | ConvertTo-Json -Depth 10)
