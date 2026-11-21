<#
.SYNOPSIS
Starts the production Compose stack after the Docker Engine is available.

.DESCRIPTION
Generic Windows logon task entry point. Unlike start-stack.ps1, this script does
not boot a particular WSL distribution or invoke a machine-specific Docker hook.
The Docker Engine itself must already be configured to start with Windows/logon.
It never removes containers, networks, or volumes.
#>
[CmdletBinding()]
param(
    [int]$DockerTimeoutSeconds = 300,
    [int]$ApiTimeoutSeconds = 240,
    [string]$ApiUrl = 'http://127.0.0.1/api/assumptions'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $env:LOCALAPPDATA 'personal-finance-planner\logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$logFile = Join-Path $logDir 'start-compose-stack.log'

function Write-Log([string]$Message) {
    Add-Content -Path $logFile -Value ('{0} {1}' -f (Get-Date -Format 'yyyy-MM-ddTHH:mm:ssK'), $Message) -Encoding utf8
}

try {
    Write-Log 'begin'
    $deadline = (Get-Date).AddSeconds($DockerTimeoutSeconds)
    do {
        & docker info *> $null
        if ($LASTEXITCODE -eq 0) { break }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)

    & docker info *> $null
    if ($LASTEXITCODE -ne 0) { throw "Docker Engine did not become ready within $DockerTimeoutSeconds seconds. Start/configure Docker Desktop or the WSL daemon first." }

    Push-Location $repoRoot
    try {
        & docker compose up -d 2>&1 | ForEach-Object { Write-Log "compose: $_" }
        if ($LASTEXITCODE -ne 0) { throw 'docker compose up -d failed.' }
    }
    finally { Pop-Location }

    $apiDeadline = (Get-Date).AddSeconds($ApiTimeoutSeconds)
    $apiReady = $false
    while ((Get-Date) -lt $apiDeadline) {
        try {
            $response = Invoke-WebRequest -Uri $ApiUrl -TimeoutSec 5
            if ($response.StatusCode -eq 200) { $apiReady = $true; break }
        }
        catch { }
        Start-Sleep -Seconds 5
    }
    if (-not $apiReady) { throw "API $ApiUrl did not answer HTTP 200 within $ApiTimeoutSeconds seconds." }
    Write-Log 'stack is up and answering'
    exit 0
}
catch {
    Write-Log "FAILED - $($_.Exception.Message)"
    exit 1
}
