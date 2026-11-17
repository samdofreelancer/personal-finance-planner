[CmdletBinding()]
param(
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$scannerImage = 'sonarsource/sonar-scanner-cli:12.4.0.4321_8.4.0'

$tokenPtr = [IntPtr]::Zero
$tokenPlain = $null
$originalToken = $env:SONAR_TOKEN
if ([string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
    $tokenSecure = Read-Host 'Enter the SonarQube project analysis token (input is hidden)' -AsSecureString
    try {
        $tokenPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($tokenSecure)
        $tokenPlain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($tokenPtr)
        $env:SONAR_TOKEN = $tokenPlain
    }
    finally {
        if ($tokenPtr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($tokenPtr)
        }
        if ($tokenSecure) { $tokenSecure.Dispose() }
        $tokenPlain = $null
    }
}
if ([string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
    throw 'SONAR_TOKEN is required. Create a project analysis token in the local SonarQube UI.'
}

try {
    $sonarStatus = Invoke-RestMethod -Uri 'http://localhost:9000/api/system/status' -TimeoutSec 5
}
catch {
    throw 'SonarQube is not reachable at http://localhost:9000. Start it with docker compose -f docker-compose.sonar.yml up -d.'
}
if ($sonarStatus.status -ne 'UP') {
    throw "SonarQube is not ready (status: $($sonarStatus.status)). Wait until /api/system/status reports UP, then retry."
}

Push-Location $repoRoot
try {
    if (-not $SkipTests) {
        Push-Location (Join-Path $repoRoot 'backend')
        try {
            mvn --batch-mode --no-transfer-progress clean verify
            if ($LASTEXITCODE -ne 0) { throw "Maven verify failed with exit code $LASTEXITCODE" }
        }
        finally { Pop-Location }

        Push-Location (Join-Path $repoRoot 'frontend')
        try {
            npm ci
            if ($LASTEXITCODE -ne 0) { throw "npm ci failed with exit code $LASTEXITCODE" }
            npm test
            if ($LASTEXITCODE -ne 0) { throw "Frontend tests/coverage failed with exit code $LASTEXITCODE" }
        }
        finally { Pop-Location }
    }

    if (-not (Test-Path (Join-Path $repoRoot 'backend/target/site/jacoco/jacoco.xml'))) {
        throw 'JaCoCo XML report is missing. Run backend Maven verify first.'
    }
    if (-not (Test-Path (Join-Path $repoRoot 'backend/target/classes'))) {
        throw 'Compiled backend classes are missing. Run backend Maven verify first.'
    }
    if (-not (Test-Path (Join-Path $repoRoot 'frontend/coverage/lcov.info'))) {
        throw 'Frontend LCOV report is missing. Run frontend npm test first.'
    }

    docker run --rm `
        --network finance-sonar-network `
        -e 'SONAR_HOST_URL=http://sonarqube:9000' `
        -e 'SONAR_TOKEN' `
        -v "${repoRoot}:/usr/src" `
        -w /usr/src `
        $scannerImage
    $scannerExitCode = $LASTEXITCODE
    if ($scannerExitCode -ne 0) {
        Write-Warning 'SonarScanner failed. Retrieving Quality Gate conditions from the local SonarQube server...'
        try {
            $headers = @{ Authorization = "Bearer $env:SONAR_TOKEN" }
            $gate = Invoke-RestMethod -Uri 'http://localhost:9000/api/qualitygates/project_status?projectKey=personal-finance-planner' -Headers $headers -TimeoutSec 15
            Write-Host "Quality Gate: $($gate.projectStatus.status)"
            $failedConditions = @($gate.projectStatus.conditions | Where-Object { $_.status -eq 'ERROR' })
            if ($failedConditions.Count -gt 0) {
                Write-Host 'Failed conditions:'
                foreach ($condition in $failedConditions) {
                    Write-Host (" - {0}: actual={1}, operator={2}, threshold={3}" -f $condition.metricKey, $condition.actualValue, $condition.comparator, $condition.errorThreshold)
                }
            }
            else {
                Write-Host 'No failed conditions were returned. Check the SonarQube project dashboard for processing or permission details.'
            }
        }
        catch {
            Write-Warning "Could not retrieve Quality Gate details automatically: $($_.Exception.Message)"
            Write-Host 'Open http://localhost:9000/dashboard?id=personal-finance-planner and inspect the failed conditions.'
        }
        throw "SonarScanner failed with exit code $scannerExitCode"
    }
}
finally {
    $env:SONAR_TOKEN = $originalToken
    Pop-Location
}
