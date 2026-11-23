# 15. Backup, deployment & operations

## PostgreSQL backup policy

- PostgreSQL is the source of truth for accounts, households and financial records. Redis is intentionally not included in backups: its sessions are bearer credentials and can be recreated by signing in again.
- Create one verified PostgreSQL custom-format archive per day. Keep 30 days locally by default; retain an encrypted off-device copy for disaster recovery. Adjust retention to available encrypted storage and recovery requirements.
- Backup archives and SHA-256 sidecars are written to `backups/postgres/`, which is excluded from Git. Treat both the archive and any exported CSV as sensitive household financial data. Use BitLocker or another encrypted volume; do not sync plaintext backups to a public/shared folder.
- The backup script restricts the backup directory and files to the current Windows user, SYSTEM and local Administrators. This is access control, not encryption; enable BitLocker on the backup volume or choose an encrypted destination to protect against device/disk theft.
- `pg_restore --list` validates the archive catalog and SHA-256 detects accidental corruption/tampering. Neither proves that a full restore will succeed; perform a restore drill against a separate empty database periodically.

Create a backup immediately:

```powershell
./scripts/backup-postgres.ps1
```

Register a daily 02:30 backup task for the currently signed-in Windows user:

```powershell
./scripts/register-postgres-backup-task.ps1 -At '02:30' -RetentionDays 30
Get-ScheduledTask -TaskName 'Planwise PostgreSQL Daily Backup'
Get-ScheduledTaskInfo -TaskName 'Planwise PostgreSQL Daily Backup'
```

The scheduled task requires the Docker Engine/Desktop to be running and the user to be signed in. `StartWhenAvailable` runs a missed task after the next logon. Verify its first execution and monitor the `backups/postgres/` directory; a scheduled task is not a substitute for an off-device copy.

## Restore procedure

Restore is a high-impact operation. Prefer a new, empty PostgreSQL database/container and verify the backup before switching the application to it. Never experiment against the active household database.

```powershell
# Inspect archive and checksum first.
Get-FileHash .\backups\postgres\planwise-postgres-YYYYMMDD-HHMMSS.dump -Algorithm SHA256

# The script checks the .sha256 sidecar and refuses a non-empty public schema by default.
# Review the target container/database carefully before confirming the operation.
./scripts/restore-postgres.ps1 -BackupPath .\backups\postgres\planwise-postgres-YYYYMMDD-HHMMSS.dump -WhatIf
./scripts/restore-postgres.ps1 -BackupPath .\backups\postgres\planwise-postgres-YYYYMMDD-HHMMSS.dump
```

`-AllowNonEmptyDatabase` bypasses the empty-database guard and should be used only for a deliberate, separately backed-up recovery plan. It does not make an in-place restore safe. After restore, verify Flyway history, user/household counts and representative financial records, then validate application reads/forecasts before allowing writes. Do not use `docker compose down -v` as a recovery shortcut.

## Local deployment and rollback

Use the guarded local deployment script:

```powershell
./scripts/deploy-local.ps1
```

It validates Compose, creates and verifies a PostgreSQL backup, tags the currently running backend/frontend images for rollback, builds the new images, updates only backend/frontend, and checks both container health plus backend readiness and frontend HTTP 200. PostgreSQL and Redis containers/volumes are not deliberately recreated by this procedure.

If an application health check fails, the script restores the previous backend/frontend image tags and recreates those two services. This is an application-image rollback only. Flyway migrations are forward-only and are not automatically reversed; if a release has changed the schema, inspect migration state and use a separately planned database recovery. Never automatically restore a database backup over the live database.

## CI and image publishing

- Pull requests and pushes run backend unit/coverage, backend integration, frontend test/build and isolated browser E2E. The Docker-build job runs only after all verification jobs pass.
- Push a version tag such as `v1.2.0` to publish `backend` and `frontend` images to GitHub Container Registry (GHCR). The `latest` tag is updated by that release workflow.
- GHCR publication is not a remote deployment. This repository has no configured production host, deployment credentials or remote environment. Configure those explicitly before automating a cloud deployment.

## Operational checks

```powershell
./scripts/health-report.ps1
docker compose logs --tail 100 backend redis db frontend
```

The report includes Compose health, backend readiness, Redis AOF status, CPU/memory snapshot and Docker disk usage. Docker JSON logs rotate at 10 MB per file and five files per service. This is local diagnostics, not centralized logging, historical metrics or alerting. If the app is later hosted on a server, add a managed monitoring/alerting destination and an external backup target before treating it as production-operated.

## Recovery objectives

Until a remote backup target and restore drill are configured, do not claim a guaranteed RPO/RTO. With a verified daily backup, the nominal local backup point is at most one day old only when the scheduled task has actually succeeded. Restore time depends on database size, disk performance and validation; measure it in a restore drill before setting an RTO.
