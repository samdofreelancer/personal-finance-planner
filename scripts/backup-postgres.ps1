[CmdletBinding()]
param(
    [string]$Container = 'finance-planner-db',
    [string]$BackupDirectory = (Join-Path (Split-Path -Parent $PSScriptRoot) 'backups\postgres'),
    [ValidateRange(1, 3650)][int]$RetentionDays = 30
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker CLI is required.' }
$containerState = docker inspect --format '{{.State.Running}}' $Container 2>$null
if ($LASTEXITCODE -ne 0 -or $containerState -ne 'true') { throw "PostgreSQL container '$Container' is not running." }
$databaseUser = docker exec $Container printenv POSTGRES_USER
if ($LASTEXITCODE -ne 0 -or -not $databaseUser) { throw 'Could not read PostgreSQL user from the container environment.' }
$databaseName = docker exec $Container printenv POSTGRES_DB
if ($LASTEXITCODE -ne 0 -or -not $databaseName) { throw 'Could not read PostgreSQL database name from the container environment.' }

New-Item -ItemType Directory -Path $BackupDirectory -Force | Out-Null
$currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
icacls $BackupDirectory /inheritance:r /grant:r "${currentUser}:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' 'BUILTIN\Administrators:(OI)(CI)F' /T /C | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Could not restrict backup folder permissions to the current user, SYSTEM and local administrators.' }
$repoRoot = Split-Path -Parent $PSScriptRoot
$stagingDirectory = Join-Path $repoRoot '.backup-staging'
New-Item -ItemType Directory -Path $stagingDirectory -Force | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$fileName = "planwise-postgres-$timestamp.dump"
$localPath = Join-Path $BackupDirectory $fileName
$containerPath = "/tmp/$fileName"

try {
    Write-Host 'Creating PostgreSQL custom-format backup...'
    docker exec $Container pg_dump --format=custom --no-owner --no-acl --username=$databaseUser --dbname=$databaseName --file=$containerPath
    if ($LASTEXITCODE -ne 0) { throw 'pg_dump failed.' }

    Push-Location $repoRoot
    try { docker cp "${Container}:$containerPath" ".backup-staging/$fileName" }
    finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy the database archive from the container.' }
    Move-Item -LiteralPath (Join-Path $stagingDirectory $fileName) -Destination $localPath -Force
    icacls $localPath /inheritance:r /grant:r "${currentUser}:F" 'SYSTEM:F' 'BUILTIN\Administrators:F' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not restrict permissions on the database archive.' }
    if (-not (Test-Path -LiteralPath $localPath) -or (Get-Item -LiteralPath $localPath).Length -eq 0) {
        throw 'Backup archive is missing or empty.'
    }

    Write-Host 'Validating archive catalog...'
    docker exec $Container pg_restore --list $containerPath | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'pg_restore archive validation failed.' }

    $hash = Get-FileHash -LiteralPath $localPath -Algorithm SHA256
    Set-Content -LiteralPath "$localPath.sha256" -Value "$($hash.Hash)  $fileName" -Encoding ascii
    icacls "$localPath.sha256" /inheritance:r /grant:r "${currentUser}:F" 'SYSTEM:F' 'BUILTIN\Administrators:F' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not restrict permissions on the backup checksum.' }

    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    Get-ChildItem -LiteralPath $BackupDirectory -Filter 'planwise-postgres-*.dump' -File |
        Where-Object { $_.LastWriteTime -lt $cutoff } |
        ForEach-Object {
            Remove-Item -LiteralPath $_.FullName -Force
            Remove-Item -LiteralPath "$($_.FullName).sha256" -Force -ErrorAction SilentlyContinue
        }

    Write-Host "Backup verified: $localPath"
    Write-Host "Size: $((Get-Item -LiteralPath $localPath).Length) bytes"
    Write-Host "SHA256: $($hash.Hash)"
    Write-Warning 'This archive contains sensitive household financial data. Store it on an encrypted disk and keep an off-device copy.'
}
finally {
    docker exec $Container rm -f $containerPath 2>$null | Out-Null
    Remove-Item -LiteralPath $stagingDirectory -Recurse -Force -ErrorAction SilentlyContinue
}
