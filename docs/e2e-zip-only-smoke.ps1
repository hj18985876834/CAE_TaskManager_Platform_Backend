param(
    [string]$GatewayBaseUrl = "http://localhost:8080",
    [string]$Username = "demo",
    [string]$Password = "123456",
    [long]$SolverId = 1,
    [long]$ProfileId = 1,
    [string]$TaskType = "SIMULATION",
    [int]$Priority = 1,
    [string]$WorkDir = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($WorkDir)) {
    $WorkDir = Join-Path $env:TEMP "cae-ziponly-smoke"
}

if (Test-Path $WorkDir) {
    Remove-Item -Recurse -Force $WorkDir
}
New-Item -ItemType Directory -Path $WorkDir | Out-Null

$script:Results = New-Object System.Collections.Generic.List[object]
$script:HasStoragePathDuplication = $false

Add-Type -AssemblyName System.Net.Http

function Add-CaseResult {
    param(
        [string]$Case,
        [bool]$Passed,
        [string]$Detail
    )
    $script:Results.Add([pscustomobject]@{
        Case = $Case
        Passed = $Passed
        Detail = $Detail
    }) | Out-Null
}

function Invoke-JsonApi {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers,
        [object]$Body
    )

    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
        ContentType = "application/json"
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }

    try {
        $resp = Invoke-RestMethod @params
        return [pscustomobject]@{ Ok = $true; StatusCode = 200; Body = $resp; Raw = $null }
    } catch {
        $statusCode = -1
        $raw = ""
        if ($_.Exception.Response) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode
            } catch {
                $statusCode = -1
            }
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $raw = $reader.ReadToEnd()
                $reader.Close()
            } catch {
                $raw = $_.Exception.Message
            }
        } else {
            $raw = $_.Exception.Message
        }
        $body = $null
        try {
            if ($raw) {
                $body = $raw | ConvertFrom-Json
            }
        } catch {
            $body = $null
        }
        return [pscustomobject]@{ Ok = $false; StatusCode = $statusCode; Body = $body; Raw = $raw }
    }
}

function Invoke-TaskFileUpload {
    param(
        [string]$Url,
        [hashtable]$Headers,
        [string]$FilePath,
        [string]$FileKey,
        [string]$FileRole
    )

    $handler = New-Object System.Net.Http.HttpClientHandler
    $client = New-Object System.Net.Http.HttpClient -ArgumentList $handler
    $request = New-Object System.Net.Http.HttpRequestMessage -ArgumentList ([System.Net.Http.HttpMethod]::Post), $Url
    $multipart = New-Object System.Net.Http.MultipartFormDataContent

    try {
        foreach ($key in $Headers.Keys) {
            $request.Headers.TryAddWithoutValidation($key, [string]$Headers[$key]) | Out-Null
        }

        $bytes = [System.IO.File]::ReadAllBytes($FilePath)
        $fileContent = New-Object System.Net.Http.ByteArrayContent -ArgumentList (, $bytes)
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/octet-stream")
        $multipart.Add($fileContent, "file", [System.IO.Path]::GetFileName($FilePath))
        $multipart.Add((New-Object System.Net.Http.StringContent -ArgumentList $FileKey), "fileKey")
        $multipart.Add((New-Object System.Net.Http.StringContent -ArgumentList $FileRole), "fileRole")

        $request.Content = $multipart

        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $statusCode = [int]$response.StatusCode
        $raw = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

        $body = $null
        try {
            if ($raw) {
                $body = $raw | ConvertFrom-Json
            }
        } catch {
            $body = $null
        }

        return [pscustomobject]@{
            Ok = ($statusCode -ge 200 -and $statusCode -lt 300)
            StatusCode = $statusCode
            Body = $body
            Raw = $raw
        }
    } catch {
        return [pscustomobject]@{
            Ok = $false
            StatusCode = -1
            Body = $null
            Raw = $_.Exception.Message
        }
    } finally {
        $multipart.Dispose()
        $request.Dispose()
        $client.Dispose()
        $handler.Dispose()
    }
}

function New-ValidZip {
    param([string]$Path)

    $inputRoot = Join-Path $WorkDir "valid_case"
    $systemDir = Join-Path $inputRoot "system"
    $constantDir = Join-Path $inputRoot "constant"
    $zeroDir = Join-Path $inputRoot "0"
    New-Item -ItemType Directory -Path $systemDir -Force | Out-Null
    New-Item -ItemType Directory -Path $constantDir -Force | Out-Null
    New-Item -ItemType Directory -Path $zeroDir -Force | Out-Null
    Set-Content -Path (Join-Path $systemDir "controlDict") -Value "application simpleFoam;" -Encoding UTF8
    Set-Content -Path (Join-Path $constantDir "transportProperties") -Value "nu [0 2 -1 0 0 0 0] 1e-05;" -Encoding UTF8
    Set-Content -Path (Join-Path $zeroDir "U") -Value "internalField uniform (0 0 0);" -Encoding UTF8

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::CreateFromDirectory($inputRoot, $Path)
}

function New-UnsafeZip {
    param([string]$Path)

    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    $zipStream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Create)
    try {
        $zip = New-Object System.IO.Compression.ZipArchive($zipStream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
        try {
            $entry = $zip.CreateEntry("../evil.txt")
            $writer = New-Object System.IO.StreamWriter($entry.Open())
            $writer.Write("unsafe")
            $writer.Dispose()
        } finally {
            $zip.Dispose()
        }
    } finally {
        $zipStream.Dispose()
    }
}

Write-Host "=== ZIP_ONLY smoke started ==="
Write-Host "Gateway: $GatewayBaseUrl"
Write-Host "WorkDir: $WorkDir"

$validZip = Join-Path $WorkDir "valid-case.zip"
$invalidSuffix = Join-Path $WorkDir "invalid-suffix.rar"
$unsafeZip = Join-Path $WorkDir "unsafe-path.zip"

New-ValidZip -Path $validZip
Set-Content -Path $invalidSuffix -Value "not a zip" -Encoding UTF8
New-UnsafeZip -Path $unsafeZip

# 1) login
$loginResp = Invoke-JsonApi -Method "POST" -Url "$GatewayBaseUrl/api/auth/login" -Headers @{} -Body @{
    username = $Username
    password = $Password
}

if (-not $loginResp.Ok -or -not $loginResp.Body -or $loginResp.Body.code -ne 0) {
    Add-CaseResult -Case "TC-01 login" -Passed $false -Detail "login failed"
    $script:Results | Format-Table -AutoSize
    exit 1
}

$token = $loginResp.Body.data.token
$userId = if ($loginResp.Body.data.userId) { [long]$loginResp.Body.data.userId } else { [long]$loginResp.Body.data.id }
$authHeaders = @{ Authorization = "Bearer $token"; "X-User-Id" = "$userId" }
Add-CaseResult -Case "TC-01 login" -Passed $true -Detail "userId=$userId"

# 2) upload-spec
$specResp = Invoke-JsonApi -Method "GET" -Url "$GatewayBaseUrl/api/profiles/$ProfileId/upload-spec" -Headers $authHeaders -Body $null
if ($specResp.Ok -and $specResp.Body.code -eq 0 -and $specResp.Body.data.uploadMode -eq "ZIP_ONLY") {
    Add-CaseResult -Case "TC-02 upload-spec" -Passed $true -Detail "uploadMode=ZIP_ONLY"
} else {
    Add-CaseResult -Case "TC-02 upload-spec" -Passed $false -Detail "upload-spec check failed"
}

# 3) create task helper
function New-Task {
    $createResp = Invoke-JsonApi -Method "POST" -Url "$GatewayBaseUrl/api/tasks" -Headers $authHeaders -Body @{
        taskName = "zip-only-smoke-$(Get-Date -Format yyyyMMddHHmmssfff)"
        solverId = $SolverId
        profileId = $ProfileId
        taskType = $TaskType
        priority = $Priority
        params = @{ smoke = "true" }
    }
    if ($createResp.Ok -and $createResp.Body.code -eq 0) {
        return [long]$createResp.Body.data.taskId
    }
    throw "create task failed"
}

function Get-IssueSummary {
    param([object]$ValidateBody)
    if ($null -eq $ValidateBody -or $null -eq $ValidateBody.data -or $null -eq $ValidateBody.data.issues) {
        return "no-issues"
    }
    $pairs = @()
    foreach ($i in $ValidateBody.data.issues) {
        $pairs += ("{0}:{1}" -f $i.errorCode, $i.path)
    }
    if ($pairs.Count -eq 0) {
        return "no-issues"
    }
    return ($pairs -join ";")
}

# 3) happy path: create -> upload(valid zip) -> validate -> submit
$taskIdHappy = New-Task

try {
    $uploadHappy = Invoke-TaskFileUpload -Url "$GatewayBaseUrl/api/tasks/$taskIdHappy/files" -Headers $authHeaders -FilePath $validZip -FileKey "input_archive" -FileRole "ARCHIVE"
    if ($uploadHappy.Ok -and $uploadHappy.Body -and $uploadHappy.Body.code -eq 0) {
        Add-CaseResult -Case "TC-03 upload valid zip" -Passed $true -Detail "taskId=$taskIdHappy"

        $postUploadFiles = Invoke-JsonApi -Method "GET" -Url "$GatewayBaseUrl/api/tasks/$taskIdHappy/files" -Headers $authHeaders -Body $null
        if ($postUploadFiles.Ok -and $postUploadFiles.Body.code -eq 0 -and $postUploadFiles.Body.data) {
            $archiveRow = @($postUploadFiles.Body.data | Where-Object { $_.fileKey -eq "input_archive" }) | Select-Object -First 1
            if ($archiveRow -and ([string]$archiveRow.storagePath -match "data/tasks/data/tasks")) {
                $script:HasStoragePathDuplication = $true
                Add-CaseResult -Case "WARN storagePath" -Passed $false -Detail "Detected duplicated root prefix in storagePath; restart task-service with latest build."
            }
        }
    } else {
        Add-CaseResult -Case "TC-03 upload valid zip" -Passed $false -Detail "status=$($uploadHappy.StatusCode) raw=$($uploadHappy.Raw)"
    }
} catch {
    Add-CaseResult -Case "TC-03 upload valid zip" -Passed $false -Detail $_.Exception.Message
}

$validateHappy = Invoke-JsonApi -Method "POST" -Url "$GatewayBaseUrl/api/tasks/$taskIdHappy/validate" -Headers $authHeaders -Body $null
if ($validateHappy.Ok -and $validateHappy.Body.code -eq 0 -and $validateHappy.Body.data.valid -eq $true) {
    Add-CaseResult -Case "TC-04 validate happy" -Passed $true -Detail "status=$($validateHappy.Body.data.status)"
} else {
    Add-CaseResult -Case "TC-04 validate happy" -Passed $false -Detail "validate failed: $(Get-IssueSummary -ValidateBody $validateHappy.Body)"
}

$submitHappy = Invoke-JsonApi -Method "POST" -Url "$GatewayBaseUrl/api/tasks/$taskIdHappy/submit" -Headers $authHeaders -Body $null
if ($submitHappy.Ok -and $submitHappy.Body.code -eq 0) {
    Add-CaseResult -Case "TC-05 submit happy" -Passed $true -Detail "submitted"
} else {
    Add-CaseResult -Case "TC-05 submit happy" -Passed $false -Detail "submit failed"
}

# 4) invalid suffix case
$taskIdBadSuffix = New-Task
$uploadBadSuffix = Invoke-TaskFileUpload -Url "$GatewayBaseUrl/api/tasks/$taskIdBadSuffix/files" -Headers $authHeaders -FilePath $invalidSuffix -FileKey "input_archive" -FileRole "ARCHIVE"
$invalidSuffixPassed = -not ($uploadBadSuffix.Ok -and $uploadBadSuffix.Body -and $uploadBadSuffix.Body.code -eq 0)
Add-CaseResult -Case "TC-06 invalid suffix reject" -Passed $invalidSuffixPassed -Detail "taskId=$taskIdBadSuffix status=$($uploadBadSuffix.StatusCode)"

# 5) missing archive validate issues
$taskIdMissing = New-Task
$validateMissing = Invoke-JsonApi -Method "POST" -Url "$GatewayBaseUrl/api/tasks/$taskIdMissing/validate" -Headers $authHeaders -Body $null
$missingIssue = $false
if ($validateMissing.Body -and $validateMissing.Body.data -and $validateMissing.Body.data.issues) {
    $missingIssue = @($validateMissing.Body.data.issues | Where-Object { $_.errorCode -eq "ARCHIVE_MISSING" }).Count -gt 0
}
Add-CaseResult -Case "TC-07 missing archive issue" -Passed $missingIssue -Detail "taskId=$taskIdMissing"

# 6) unsafe archive path
$taskIdUnsafe = New-Task
$unsafeUploadResp = Invoke-TaskFileUpload -Url "$GatewayBaseUrl/api/tasks/$taskIdUnsafe/files" -Headers $authHeaders -FilePath $unsafeZip -FileKey "input_archive" -FileRole "ARCHIVE"
$uploadedUnsafe = ($unsafeUploadResp.Ok -and $unsafeUploadResp.Body -and $unsafeUploadResp.Body.code -eq 0)

$unsafeIssue = $false
if ($uploadedUnsafe) {
    $validateUnsafe = Invoke-JsonApi -Method "POST" -Url "$GatewayBaseUrl/api/tasks/$taskIdUnsafe/validate" -Headers $authHeaders -Body $null
    if ($validateUnsafe.Body -and $validateUnsafe.Body.data -and $validateUnsafe.Body.data.issues) {
        $unsafeIssue = @($validateUnsafe.Body.data.issues | Where-Object { $_.errorCode -eq "ARCHIVE_UNSAFE_PATH" }).Count -gt 0
        if (-not $unsafeIssue) {
            Add-CaseResult -Case "TC-08 detail" -Passed $false -Detail "unexpected issues: $(Get-IssueSummary -ValidateBody $validateUnsafe.Body)"
        }
    }
}
Add-CaseResult -Case "TC-08 unsafe path issue" -Passed $unsafeIssue -Detail "taskId=$taskIdUnsafe"

# 7) metadata check after happy validate
$filesResp = Invoke-JsonApi -Method "GET" -Url "$GatewayBaseUrl/api/tasks/$taskIdHappy/files" -Headers $authHeaders -Body $null
$metaPassed = $false
if ($filesResp.Ok -and $filesResp.Body.code -eq 0 -and $filesResp.Body.data) {
    $archiveRow = @($filesResp.Body.data | Where-Object { $_.fileKey -eq "input_archive" }) | Select-Object -First 1
    if ($archiveRow) {
        $metaPassed = ($archiveRow.archiveFlag -eq 1 -and -not [string]::IsNullOrWhiteSpace([string]$archiveRow.unpackDir))
    }
}
Add-CaseResult -Case "TC-09 archive metadata persisted" -Passed $metaPassed -Detail "taskId=$taskIdHappy"

Write-Host ""
Write-Host "=== ZIP_ONLY smoke summary ==="
$script:Results | Format-Table -AutoSize

$failedCount = @($script:Results | Where-Object { -not $_.Passed }).Count
if ($failedCount -gt 0) {
    Write-Host ""
    Write-Host "FAILED: $failedCount case(s) failed." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "SUCCESS: all ZIP_ONLY smoke cases passed." -ForegroundColor Green
exit 0
