[CmdletBinding()]
param(
    [ValidateRange(30, 600)][int]$HealthTimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
$rollbackPrepared = $false

function Test-ServiceHealthy([string]$Service) {
    $status = docker inspect --format '{{.State.Health.Status}}' "finance-planner-$Service" 2>$null
    return $LASTEXITCODE -eq 0 -and $status -eq 'healthy'
}

try {
    docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Compose configuration validation failed.' }

    # A verified PostgreSQL archive is mandatory before any image/schema rollout.
    & (Join-Path $PSScriptRoot 'backup-postgres.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Pre-deploy PostgreSQL backup failed; deployment aborted.' }

    foreach ($service in @('backend', 'frontend')) {
        docker inspect "finance-planner-$service" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Existing $service container not found; refusing an unprotected deployment." }
        docker image inspect "planwise/${service}:rollback" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Verified rollback image planwise/${service}:rollback is missing; refusing deployment." }
    }
    $rollbackPrepared = $true

    Write-Host 'Building backend and frontend images...'
    docker compose build backend frontend
    if ($LASTEXITCODE -ne 0) { throw 'Image build failed.' }

    Write-Host 'Updating backend and frontend; PostgreSQL and Redis containers/volumes are left untouched...'
    docker compose up -d --no-deps backend frontend
    if ($LASTEXITCODE -ne 0) { throw 'Compose update failed.' }

    $deadline = (Get-Date).AddSeconds($HealthTimeoutSeconds)
    do {
        $backendHealthy = Test-ServiceHealthy 'backend'
        $frontendHealthy = Test-ServiceHealthy 'frontend'
        if ($backendHealthy -and $frontendHealthy) { break }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)

    if (-not $backendHealthy -or -not $frontendHealthy) { throw 'Backend/frontend did not become healthy before timeout.' }
    $readiness = docker exec finance-planner-backend wget -qO- http://127.0.0.1:8080/actuator/health/readiness
    if ($LASTEXITCODE -ne 0 -or $readiness -notmatch '"status":"UP"') { throw 'Backend readiness check failed.' }
    $response = Invoke-WebRequest -Uri 'http://127.0.0.1/' -TimeoutSec 15
    if ($response.StatusCode -ne 200) { throw "Frontend returned HTTP $($response.StatusCode)." }

    Write-Host 'Deployment successful. Backend/frontend healthy; database backup verified; PostgreSQL and Redis volumes preserved.' -ForegroundColor Green
    docker compose ps
}
catch {
    Write-Warning "Deployment failed: $($_.Exception.Message)"
    Write-Warning 'Attempting application-image rollback. Database migrations are not automatically reversed; inspect Flyway logs and restore only through the documented recovery procedure if required.'
    if ($rollbackPrepared) {
        foreach ($service in @('backend', 'frontend')) {
            docker image tag "planwise/${service}:rollback" "planwise/${service}:local" 2>$null
        }
        docker compose up -d --no-deps --no-build backend frontend
        if ($LASTEXITCODE -ne 0) { Write-Warning 'Application image rollback command failed; manual recovery is required.' }
    } else {
        Write-Warning 'Deployment stopped before image replacement; existing application containers were left unchanged.'
    }
    throw
}
finally {
    Pop-Location
}
