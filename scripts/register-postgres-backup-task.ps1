[CmdletBinding()]
param(
    [string]$TaskName = 'Planwise PostgreSQL Daily Backup',
    [ValidatePattern('^([01]\d|2[0-3]):[0-5]\d$')][string]$At = '02:30',
    [ValidateRange(1, 3650)][int]$RetentionDays = 30
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$backupScript = Join-Path $PSScriptRoot 'backup-postgres.ps1'
$powershell = (Get-Command pwsh -ErrorAction SilentlyContinue).Source
if (-not $powershell) { $powershell = (Get-Command powershell -ErrorAction Stop).Source }

$action = New-ScheduledTaskAction -Execute $powershell -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$backupScript`" -RetentionDays $RetentionDays" -WorkingDirectory $repoRoot
$trigger = New-ScheduledTaskTrigger -Daily -At $At
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Hours 2)
$principal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Description 'Creates and verifies a PostgreSQL custom-format Planwise backup; keeps the configured retention window.' -Force | Out-Null
Write-Host "Registered '$TaskName' at $At daily for the current interactive user."
Write-Warning 'Docker Desktop/Engine must be running and this user must be signed in. Verify the first run and store backups on an encrypted disk.'
