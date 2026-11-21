<#
.SYNOPSIS
Boots the WSL2 Docker Engine (when needed) and brings the Compose stack up.

.DESCRIPTION
Used by the Windows logon Scheduled Task "PersonalFinancePlanner-Stack" so the
planner is reachable after an OS restart. Safe to run manually and repeatedly.

The Docker Engine of this machine runs inside the WSL2 distribution, so the
distribution has to be booted before any container can start. Booting it also
runs the /etc/wsl.conf boot hook (/usr/local/sbin/docker-boot) that starts
dockerd; this script waits for the engine and falls back to invoking the hook
explicitly when the boot hook did not manage to start it.

It does not start/stop other Compose projects and never deletes volumes.
#>
[CmdletBinding()]
param(
    [string]$Distro = 'Ubuntu',
    [int]$DockerTimeoutSeconds = 180,
    [int]$ApiTimeoutSeconds = 180,
    [string]$ApiUrl = 'http://localhost/api/assumptions'
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $env:LOCALAPPDATA 'personal-finance-planner\logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$logFile = Join-Path $logDir 'start-stack.log'

function Write-Log([string]$Message) {
    $line = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-ddTHH:mm:ssK'), $Message
    Add-Content -Path $logFile -Value $line -Encoding utf8
}

function Test-DockerEngine {
    & docker info *> $null
    return ($LASTEXITCODE -eq 0)
}

function Invoke-ComposeUp {
    Push-Location $repoRoot
    try {
        & docker compose up -d 2>&1 | ForEach-Object { Write-Log "compose: $_" }
        return ($LASTEXITCODE -eq 0)
    }
    finally {
        Pop-Location
    }
}

function Write-PortOwner {
    # A published port can only belong to one container; name the squatter so the
    # log explains a conflict instead of just reporting a failed start.
    & docker ps --format '{{.Names}} {{.Ports}}' 2>&1 |
        Where-Object { $_ -match '8080' } |
        ForEach-Object { Write-Log "port 8080 held by: $_" }
}

try {
    Write-Log 'begin'

    # Booting the distribution runs the /etc/wsl.conf boot hook that starts dockerd.
    & wsl.exe -d $Distro -u root -e true *> $null
    Write-Log "distribution '$Distro' booted"

    $deadline = (Get-Date).AddSeconds($DockerTimeoutSeconds)
    while (-not (Test-DockerEngine) -and (Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 3
    }

    if (-not (Test-DockerEngine)) {
        Write-Log 'engine not ready after boot hook; invoking docker-boot explicitly'
        & wsl.exe -d $Distro -u root -e /usr/local/sbin/docker-boot *> $null
        Start-Sleep -Seconds 2
    }

    if (-not (Test-DockerEngine)) {
        throw "Docker engine did not become ready within $DockerTimeoutSeconds seconds."
    }
    Write-Log 'docker engine ready'

    if (-not (Invoke-ComposeUp)) {
        Write-PortOwner
        Write-Log 'compose up failed; retrying once in 10s'
        Start-Sleep -Seconds 10
        if (-not (Invoke-ComposeUp)) {
            Write-PortOwner
            throw 'docker compose up -d failed.'
        }
    }
    Write-Log 'containers started'

    $apiDeadline = (Get-Date).AddSeconds($ApiTimeoutSeconds)
    $apiReady = $false
    while ((Get-Date) -lt $apiDeadline) {
        try {
            $response = Invoke-WebRequest -Uri $ApiUrl -TimeoutSec 5 -SkipHttpErrorCheck
            if ($response.StatusCode -eq 200) {
                $apiReady = $true
                break
            }
        }
        catch {
            # Backend still starting; keep waiting until the deadline.
        }
        Start-Sleep -Seconds 5
    }

    if (-not $apiReady) {
        throw "API $ApiUrl did not answer 200 within $ApiTimeoutSeconds seconds."
    }

    Write-Log 'stack is up and answering'
    exit 0
}
catch {
    Write-Log "FAILED - $($_.Exception.Message)"
    exit 1
}
