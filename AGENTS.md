# Repository Agent Instructions

Read these documents before making a non-trivial change:

1. `docs/07-ai-agent-context.md` — product boundaries, source-of-truth rules and required context.
2. `docs/08-engineering-constitution.md` — engineering constraints and definition of done.
3. The relevant documents linked from `docs/README.md` — especially business rules, API contract and data model.
4. The applicable SpecKit specification under `specs/`.

## Mandatory behavior

- Inspect the current implementation before describing or changing it. Documentation is not proof that code already behaves that way.
- Do not invent product requirements, financial facts, API behavior or architecture decisions.
- Keep changes within the approved scope. Ask a focused question when a decision materially affects financial calculations, persistence, privacy or compatibility.
- Add or update automated tests for behavior changes. Prefer tests that assert observable outcomes over implementation details.
- Update affected docs and diagram sources in the same change.
- Never commit real financial data, secrets, credentials, tokens or personal identifiers.
- Do not claim tests, builds, reviews or deployments succeeded unless they were actually run and their results observed.

## Test, build, run and verify

Run commands from the repository root unless a command changes directory explicitly. The standard entrypoint is `./scripts/verify.ps1`; read `docs/06-development-guide.md` for prerequisites and details.

```powershell
./scripts/verify.ps1                         # tests + frontend build + Compose config
./scripts/verify.ps1 -DockerBuild             # also build production Docker images
./scripts/verify.ps1 -SmokeTest               # also smoke-test an already-running production-like stack
./scripts/verify.ps1 -SmokeTest -DevStack     # smoke-test an already-running dev stack (frontend :5173)
```

The script does not start/stop containers, mutate assumptions, seed the database or delete volumes. Start the desired stack explicitly before using `-SmokeTest`.

### Test

The script runs the commands below as part of its default checks:

```powershell
Push-Location backend; mvn test; Pop-Location
Push-Location frontend; npm ci; npm run build; Pop-Location
```

Report the actual test count. `mvn test` currently may report **no Java tests to run**; a successful Maven lifecycle is not proof that backend business behavior is covered.

### Build production containers

```powershell
docker compose build
```

Or use `./scripts/verify.ps1 -DockerBuild` to run all standard checks and build the images.

### Start / deploy locally

Production-like local Compose stack:

```powershell
Copy-Item .env.example .env  # first run only; set a unique local POSTGRES_PASSWORD
docker compose up -d --build
```

Hot-reload development stack:

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

This repository documents local Docker Compose deployment only. Do not infer or claim a production/cloud deployment procedure exists.

### Verify the running app

```powershell
docker compose ps
docker compose logs --tail 100 frontend backend db
Invoke-WebRequest http://localhost
Invoke-RestMethod http://localhost:8080/api/assumptions
```

For the development overlay, check the frontend at `http://localhost:5173` and use the same backend API check at port `8080`. Inspect container logs and confirm the expected services are running; do not call the app healthy based on container status alone.

### Stop safely

```powershell
docker compose down
```

Do not use `docker compose down -v` unless the user explicitly intends to delete the persisted database volume. Never reset, seed or overwrite household assumptions as part of verification without explicit authorization; seed data changes persisted state.

For changes affecting the development overlay, verify it with the exact two-file Compose command above. For changes affecting production images, verify `docker compose build` and a local stack smoke test when the environment permits.
