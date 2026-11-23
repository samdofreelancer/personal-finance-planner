# 14. Redis operations

## Purpose and data classification

Planwise uses Redis 7 only as the Spring Session store (`planwise:session`). PostgreSQL remains the source of truth for user identities, household membership and all financial data. Redis contains authentication session identifiers, security contexts and CSRF/session attributes; treat its contents as bearer credentials.

## Persistence and backup policy

- Redis runs only on the private Compose bridge network. Port `6379` is not published to the host or LAN, authentication is required, and the container drops all but the CHOWN/DAC_OVERRIDE/SETUID/SETGID capabilities needed by the official image entrypoint to set `/data` ownership before dropping to the unprivileged Redis account. Privilege escalation is disabled.
- AOF persistence is enabled and `/data` is mounted to the named `finance_planner_redis_data` Compose volume. This protects sessions against normal Redis/backend container restarts and recreation.
- **Do not export or off-site-backup Redis session contents by default.** A copied session store can contain still-valid bearer sessions. Session loss is recoverable by signing in again; no household or financial records are stored in Redis.
- Back up PostgreSQL independently and protect those backups as sensitive financial data. Redis AOF is persistence, not a substitute for the PostgreSQL backup policy.
- Login-failure throttling currently uses a bounded in-process counter (5 failures per 15-minute window). It protects a single backend instance but resets on backend restart and is not shared across multiple replicas; move it to Redis before horizontally scaling the backend.

## Normal operation checks

```powershell
docker compose ps
docker compose logs --tail 100 redis backend
docker compose exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping'
docker compose exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli INFO persistence'
```

Expected Redis response is `PONG`; `aof_enabled:1` confirms AOF is active. Backend readiness checks PostgreSQL and Redis. Liveness checks only the backend process so a Redis outage does not trigger an unnecessary liveness restart loop. Backend and frontend Compose healthchecks gate startup in dependency order.

## Recovery when Redis data is corrupt or its volume is lost

Redis stores only transient sessions. If Redis cannot recover its AOF, replace only the Redis volume and let users authenticate again. This is intentionally safer than restoring old bearer sessions.

```powershell
# Run from the repository root. Capture the exact volume mounted at /data.
$redisVolume = docker inspect finance-planner-redis --format '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Name}}{{end}}{{end}}'
if (-not $redisVolume) { throw 'Could not identify the Redis /data volume; stop and inspect Docker.' }

# Stop writers first. This does not remove PostgreSQL data.
docker compose stop backend redis
if ($LASTEXITCODE -ne 0) { throw 'Could not stop backend/Redis.' }

# Destructive only to ephemeral login sessions. Do not substitute `docker compose down -v`.
docker volume rm $redisVolume
if ($LASTEXITCODE -ne 0) { throw 'Could not remove the identified Redis volume.' }

docker compose up -d redis backend
if ($LASTEXITCODE -ne 0) { throw 'Could not recreate Redis and backend.' }
docker compose ps redis backend
```

After recovery:

1. Confirm Redis is `healthy` and responds `PONG`.
2. Confirm backend `/actuator/health/readiness` is `UP` (the endpoint is internal to the Docker network).
3. Sign in again. Existing sessions are intentionally invalidated.
4. Verify household and financial data in the app; those records remain in PostgreSQL and must not be reset or reseeded.

Never remove `finance_planner_db_data` as part of Redis recovery. Never use `docker compose down -v` for this procedure.

## CI recovery test

The isolated Playwright runner registers a synthetic account, then restarts the E2E backend and Redis containers. It verifies that the same authenticated session remains valid after both restarts. The E2E PostgreSQL and Redis volumes are separate from local application volumes.
