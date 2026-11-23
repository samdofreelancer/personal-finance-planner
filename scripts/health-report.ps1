[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    Write-Host '=== Compose service health ===' -ForegroundColor Cyan
    docker compose ps
    if ($LASTEXITCODE -ne 0) { throw 'Could not read Compose service status.' }

    Write-Host "`n=== Backend readiness ===" -ForegroundColor Cyan
    docker exec finance-planner-backend wget -qO- http://127.0.0.1:8080/actuator/health/readiness
    if ($LASTEXITCODE -ne 0) { throw 'Backend readiness endpoint is unavailable.' }

    Write-Host "`n=== Redis persistence ===" -ForegroundColor Cyan
    $redisPassword = docker exec finance-planner-redis printenv REDIS_PASSWORD
    if ($LASTEXITCODE -ne 0 -or -not $redisPassword) { throw 'Could not read Redis authentication configuration.' }
    $redisInfo = docker exec --env "REDISCLI_AUTH=$redisPassword" finance-planner-redis redis-cli INFO persistence
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect Redis AOF status.' }
    $redisInfo | Where-Object { $_ -match '^(aof_enabled|aof_last_write_status):' }

    Write-Host "`n=== Container resource snapshot ===" -ForegroundColor Cyan
    docker stats --no-stream finance-planner-backend finance-planner-frontend finance-planner-db finance-planner-redis
    if ($LASTEXITCODE -ne 0) { throw 'Could not read container resource usage.' }

    Write-Host "`n=== Docker disk usage ===" -ForegroundColor Cyan
    docker system df
    if ($LASTEXITCODE -ne 0) { throw 'Could not read Docker disk usage.' }
}
finally {
    Pop-Location
}
