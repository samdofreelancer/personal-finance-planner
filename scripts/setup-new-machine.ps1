<#
.SYNOPSIS
Prepares a Windows host and deploys Personal Finance Planner.

.DESCRIPTION
Validates installed prerequisites, creates a secure local .env when absent,
validates Compose configuration, deploys the stack, waits for health, and can
configure private Tailscale Serve plus a Windows logon task.

Database backup/restore is intentionally not automated: restoring over an
existing volume is destructive and must follow docs/13-new-machine-deployment.md.
This script never deletes or resets Docker volumes and never configures Funnel.
#>
[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'Medium')]
param(
    [switch]$ConfigureTailscale,
    [switch]$ConfigureAutoStart,
    [switch]$SkipBuild,
    [int]$DockerTimeoutSeconds = 180,
    [int]$ApiTimeoutSeconds = 240
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repoRoot '.env'
$envExample = Join-Path $repoRoot '.env.example'
$startupScript = Join-Path $PSScriptRoot 'start-compose-stack.ps1'

function Assert-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found. Install/configure it, then rerun this script."
    }
}

function New-DatabasePassword {
    $bytes = New-Object byte[] 36
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) }
    finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', 'A').Replace('/', 'B')
}

function Test-HttpReady([string]$Uri, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri $Uri -TimeoutSec 5
            if ($response.StatusCode -eq 200) { return $true }
        }
        catch { }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)
    return $false
}

Write-Host '== Personal Finance Planner: Windows host setup ==' -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"

Assert-Command 'docker'
Assert-Command 'git'

& docker version *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker Engine is not reachable. Start Docker Desktop or the configured WSL2 Docker daemon first.' }
& docker compose version *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose plugin is unavailable. Install Docker Compose v2.' }
& git -C $repoRoot rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) { throw 'The script must run from inside a Git working tree.' }

if (-not (Test-Path $envFile)) {
    if (-not (Test-Path $envExample)) { throw '.env.example is missing.' }
    $content = Get-Content -LiteralPath $envExample -Raw
    $password = New-DatabasePassword
    $content = $content.Replace('change-this-local-password', $password)
    if ($content -match 'change-this-local-password') { throw 'Could not replace the database password placeholder.' }
    if ($PSCmdlet.ShouldProcess($envFile, 'Create .env with a generated database password')) {
        Set-Content -LiteralPath $envFile -Value $content -Encoding ascii -NoNewline
        Write-Host 'Created .env with a randomly generated PostgreSQL password. Keep this file private.' -ForegroundColor Green
    }
}
else {
    $envContent = Get-Content -LiteralPath $envFile -Raw
    if ($envContent -match 'change-this-local-password') {
        throw '.env still contains the example password. Edit .env and set a unique strong POSTGRES_PASSWORD before continuing.'
    }
    Write-Host '.env already exists; leaving it unchanged.'
}

if (-not (Test-Path $envFile)) { throw '.env was not created (possibly because -WhatIf was used). No deployment was started.' }

Push-Location $repoRoot
try {
    & docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'docker compose config failed. Review .env and Compose configuration.' }

    $composeArgs = @('compose', 'up', '-d')
    if (-not $SkipBuild) { $composeArgs += '--build' }
    if ($PSCmdlet.ShouldProcess('Docker Compose application stack', ($composeArgs -join ' '))) {
        & docker @composeArgs
        if ($LASTEXITCODE -ne 0) { throw 'Docker Compose deployment failed. Inspect: docker compose logs.' }
    }
}
finally { Pop-Location }

if (-not (Test-HttpReady -Uri 'http://127.0.0.1/api/assumptions' -TimeoutSeconds $ApiTimeoutSeconds)) {
    throw "Backend API did not return HTTP 200 within $ApiTimeoutSeconds seconds. Inspect docker compose ps and docker compose logs."
}
Write-Host 'Frontend/backend health check: HTTP 200.' -ForegroundColor Green

if ($ConfigureTailscale) {
    $tailscale = Get-Command 'tailscale' -ErrorAction SilentlyContinue
    if (-not $tailscale) { throw 'Tailscale CLI is not installed. Install Tailscale, sign in to the tailnet, then rerun with -ConfigureTailscale.' }
    & tailscale status *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Tailscale is not connected. Sign in to the intended tailnet, then rerun.' }
    if ($PSCmdlet.ShouldProcess('Tailscale Serve', 'Configure private HTTPS proxy to 127.0.0.1:80')) {
        & tailscale serve --bg 80
        if ($LASTEXITCODE -ne 0) { throw 'Tailscale Serve configuration failed.' }
        & tailscale serve status
    }
}

if ($ConfigureAutoStart) {
    if (-not (Get-Module -ListAvailable -Name ScheduledTasks)) {
        throw 'ScheduledTasks PowerShell module is unavailable on this Windows installation.'
    }
    $taskName = 'PersonalFinancePlanner-Compose-Stack'
    $powerShell = Join-Path $PSHOME 'powershell.exe'
    $argument = '-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "{0}"' -f $startupScript
    $action = New-ScheduledTaskAction -Execute $powerShell -Argument $argument -WorkingDirectory $repoRoot
    $trigger = New-ScheduledTaskTrigger -AtLogOn -User ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name)
    $trigger.Delay = 'PT30S'
    $principal = New-ScheduledTaskPrincipal -UserId ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) -LogonType Interactive -RunLevel Limited
    $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Minutes 10)
    if ($PSCmdlet.ShouldProcess("Scheduled Task '$taskName'", 'Register stack startup after current user logs on')) {
        Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Description 'Starts Personal Finance Planner Compose stack after Docker Engine is available.' -Force | Out-Null
        Write-Host "Registered Scheduled Task '$taskName'. Ensure Docker Desktop/WSL Docker Engine itself starts automatically." -ForegroundColor Green
    }
}

Write-Host ''
Write-Host 'Setup steps completed.' -ForegroundColor Green
Write-Host 'Next: verify docker compose ps, open http://127.0.0.1/, and (if configured) check tailscale serve status.'
Write-Host 'Database transfer is a separate, deliberate operation. Follow docs/13-new-machine-deployment.md; this script never restores or deletes database volumes.'
