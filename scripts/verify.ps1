[CmdletBinding()]
param(
    [switch]$DockerBuild,
    [switch]$SmokeTest,
    [switch]$DevStack
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$results = [System.Collections.Generic.List[object]]::new()

function Invoke-Check {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][scriptblock]$Action
    )

    Write-Host "`n==> $Name" -ForegroundColor Cyan
    try {
        Push-Location $repoRoot
        & $Action
        if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) {
            throw "Command exited with code $LASTEXITCODE"
        }
        $results.Add([pscustomobject]@{ Check = $Name; Result = 'PASS' })
        Write-Host "PASS: $Name" -ForegroundColor Green
    }
    catch {
        $results.Add([pscustomobject]@{ Check = $Name; Result = 'FAIL'; Detail = $_.Exception.Message })
        Write-Host "FAIL: $Name - $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
    finally {
        Pop-Location
    }
}

function Assert-HttpEndpoint {
    param([Parameter(Mandatory)][string]$Uri)
    $response = Invoke-WebRequest -Uri $Uri -Method Get -TimeoutSec 10
    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 400) {
        throw "Unexpected HTTP status $($response.StatusCode) from $Uri"
    }
    Write-Host "HTTP $($response.StatusCode) $Uri"
}

try {
    foreach ($tool in @('java', 'mvn', 'node', 'npm', 'docker')) {
        if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
            throw "Required tool not found in PATH: $tool"
        }
    }

    Invoke-Check 'Backend Maven verify and JaCoCo coverage gate' {
        Push-Location (Join-Path $repoRoot 'backend')
        try { mvn verify } finally { Pop-Location }
    }

    Invoke-Check 'Backend PIT mutation testing' {
        Push-Location (Join-Path $repoRoot 'backend')
        try { mvn org.pitest:pitest-maven:mutationCoverage } finally { Pop-Location }
    }

    Invoke-Check 'Frontend unit tests and coverage' {
        Push-Location (Join-Path $repoRoot 'frontend')
        try {
            npm ci
            if ($LASTEXITCODE -ne 0) { throw "npm ci exited with code $LASTEXITCODE" }
            npm test
            if ($LASTEXITCODE -ne 0) { throw "npm test exited with code $LASTEXITCODE" }
        } finally { Pop-Location }
    }

    Invoke-Check 'Frontend production build' {
        Push-Location (Join-Path $repoRoot 'frontend')
        try {
            npm run build
            if ($LASTEXITCODE -ne 0) { throw "npm run build exited with code $LASTEXITCODE" }
        } finally { Pop-Location }
    }

    Invoke-Check 'Docker Compose configuration validation' {
        docker compose config --quiet
        if ($LASTEXITCODE -ne 0) { throw "docker compose config exited with code $LASTEXITCODE" }
    }

    Invoke-Check 'SonarQube Docker Compose configuration validation' {
        $validationFolderName = ".e2e-tmp-sonar-$([guid]::NewGuid().ToString('N'))"
        $validationDirectory = Join-Path $repoRoot $validationFolderName
        $validationEnvFile = Join-Path $validationDirectory '.env'
        try {
            # Compose validation only needs a non-empty value; never require or
            # copy the user's real Sonar credential into a verification process.
            New-Item -ItemType Directory -Path $validationDirectory -Force | Out-Null
            Set-Content -LiteralPath $validationEnvFile -Value 'SONAR_DB_PASSWORD=compose-validation-only' -Encoding ascii
            docker compose --env-file "$validationFolderName/.env" -f docker-compose.sonar.yml config --quiet
            if ($LASTEXITCODE -ne 0) { throw "SonarQube Compose config exited with code $LASTEXITCODE" }
        } finally {
            Remove-Item -LiteralPath $validationDirectory -Recurse -Force -ErrorAction SilentlyContinue
        }
    }

    if ($DockerBuild) {
        Invoke-Check 'Production Docker image build' {
            docker compose build
            if ($LASTEXITCODE -ne 0) { throw "docker compose build exited with code $LASTEXITCODE" }
        }
    }

    if ($SmokeTest) {
        $frontendUri = if ($DevStack) { 'http://localhost:5173/' } else { 'http://localhost/' }
        Invoke-Check 'Frontend HTTP smoke test' { Assert-HttpEndpoint -Uri $frontendUri }
        Invoke-Check 'Backend assumptions API smoke test' {
            $response = Invoke-RestMethod -Uri 'http://localhost:8080/api/assumptions' -Method Get -TimeoutSec 10
            if ($null -eq $response) { throw 'Backend returned an empty assumptions response.' }
            Write-Host 'GET /api/assumptions returned a response.'
        }
    }

    Write-Host "`nVerification summary" -ForegroundColor Cyan
    $results | Format-Table -AutoSize
    Write-Host 'All requested verification checks passed.' -ForegroundColor Green
}
catch {
    Write-Host "`nVerification stopped: $($_.Exception.Message)" -ForegroundColor Red
    if ($results.Count -gt 0) { $results | Format-Table -AutoSize }
    exit 1
}
