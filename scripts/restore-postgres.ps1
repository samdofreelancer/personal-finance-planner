[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'High')]
param(
    [Parameter(Mandatory)][ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })][string]$BackupPath,
    [string]$Container = 'finance-planner-db',
    [switch]$AllowNonEmptyDatabase
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker CLI is required.' }
$resolvedBackup = (Resolve-Path -LiteralPath $BackupPath).Path
$checksumPath = "$resolvedBackup.sha256"
if (-not (Test-Path -LiteralPath $checksumPath)) { throw 'SHA-256 sidecar is required; refusing to restore an unverified archive.' }
$expectedHash = ((Get-Content -LiteralPath $checksumPath -Raw).Trim() -split '\s+')[0]
$actualHash = (Get-FileHash -LiteralPath $resolvedBackup -Algorithm SHA256).Hash
if ($expectedHash -ne $actualHash) { throw 'Backup SHA-256 mismatch; archive may be corrupted or modified.' }

$containerState = docker inspect --format '{{.State.Running}}' $Container 2>$null
if ($LASTEXITCODE -ne 0 -or $containerState -ne 'true') { throw "PostgreSQL container '$Container' is not running." }
$databaseUser = docker exec $Container printenv POSTGRES_USER
if ($LASTEXITCODE -ne 0 -or -not $databaseUser) { throw 'Could not read PostgreSQL user from the container environment.' }
$databaseName = docker exec $Container printenv POSTGRES_DB
if ($LASTEXITCODE -ne 0 -or -not $databaseName) { throw 'Could not read PostgreSQL database name from the container environment.' }
$tableCount = docker exec $Container psql -U $databaseUser -d $databaseName -Atqc 'SELECT count(*) FROM pg_tables WHERE schemaname = current_schema()'
if ($LASTEXITCODE -ne 0) { throw 'Could not inspect target database; refusing to restore.' }
if ([int]$tableCount -gt 0 -and -not $AllowNonEmptyDatabase) {
    throw "Target database contains $tableCount public tables. Restore is allowed only into an empty database unless -AllowNonEmptyDatabase is explicitly supplied."
}

$fileName = [System.IO.Path]::GetFileName($resolvedBackup)
$containerPath = "/tmp/$fileName"
$repoRoot = Split-Path -Parent $PSScriptRoot
$stagingDirectory = Join-Path $repoRoot '.backup-staging'
$stagedBackup = Join-Path $stagingDirectory $fileName

try {
    if ($PSCmdlet.ShouldProcess("PostgreSQL database in $Container", "Restore $fileName")) {
        New-Item -ItemType Directory -Path $stagingDirectory -Force | Out-Null
        Copy-Item -LiteralPath $resolvedBackup -Destination $stagedBackup -Force
        Push-Location $repoRoot
        try { docker cp ".backup-staging/$fileName" "${Container}:$containerPath" }
        finally { Pop-Location }
        if ($LASTEXITCODE -ne 0) { throw 'Could not copy archive into PostgreSQL container.' }
        docker exec $Container pg_restore --list $containerPath | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Archive catalog validation failed inside PostgreSQL container.' }
        docker exec $Container pg_restore --exit-on-error --no-owner --no-acl --username=$databaseUser --role=$databaseUser --dbname=$databaseName $containerPath
        if ($LASTEXITCODE -ne 0) { throw 'pg_restore failed. Stop and inspect the target database before retrying.' }
        Write-Host 'Restore command completed. Verify Flyway state and application data before allowing writes.'
    }
}
finally {
    docker exec $Container rm -f $containerPath 2>$null | Out-Null
    Remove-Item -LiteralPath $stagingDirectory -Recurse -Force -ErrorAction SilentlyContinue
}
