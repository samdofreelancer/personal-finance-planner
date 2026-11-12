# 06. Development Guide

## Prerequisites

- Docker Engine/Desktop and Docker Compose for the full local stack.
- For isolated development: Java 21 and Maven for backend; Node.js 18+ and npm for frontend.
- PostgreSQL 16 when running backend outside Docker.

## Run with Docker Compose

```powershell
Copy-Item .env.example .env
# Set a unique local POSTGRES_PASSWORD in .env before startup.
docker compose up --build
```

Current compose publishes frontend on `http://localhost`, backend on `http://localhost:8080`, and PostgreSQL on `localhost:5432`. These are the **current** bindings, not the desired secure default. Until F-01 is implemented, do not expose this stack to an untrusted network; prefer a trusted local machine and firewall.

Stop containers but keep persisted data:

```powershell
docker compose down
```

Delete containers and the database volume (destructive):

```powershell
docker compose down -v
```

## Run services independently

Backend:

```powershell
cd backend
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

Vite development server defaults to port 5173. Backend datasource and CORS settings are configured through environment variables and `backend/src/main/resources/application.properties`.

## Verification commands

The repository verification entrypoint is `scripts/verify.ps1` (tested with Windows PowerShell 5.1; PowerShell 7 is also suitable). From the repository root:

```powershell
./scripts/verify.ps1                     # backend tests, frontend build, Compose config
./scripts/verify.ps1 -DockerBuild        # above + production image build
./scripts/verify.ps1 -SmokeTest          # above + HTTP checks against an already-running production-like stack
./scripts/verify.ps1 -SmokeTest -DevStack # use frontend port 5173 for an already-running dev stack
```

The script is intentionally non-destructive: it does not start/stop containers, seed/reset assumptions or remove database volumes. Start the stack separately before a smoke test. A smoke test checks that the frontend responds and `GET /api/assumptions` returns a response; it is not a full end-to-end financial calculation test.

Backend build/tests:

```powershell
cd backend
mvn test
```

Frontend production build:

```powershell
cd frontend
npm ci
npm run build
```

## Engineering workflow

1. Clarify behavior and acceptance criteria before implementation.
2. Update the relevant SpecKit specification; keep findings traceable to requirement IDs.
3. Review architecture impact and update this documentation/diagram when boundaries or flows change.
4. Implement with tests first where practical; keep forecast business rules covered by backend tests.
5. Run backend tests, frontend build and relevant integration/manual checks.
6. Record verification evidence and update task status only after checks pass.
7. Review security and privacy impact for every change touching deployment, logs, exports, persistence or API inputs.

## Change checklist

- [ ] Requirements and acceptance criteria are clear and testable.
- [ ] No real financial data, credentials, tokens or personal identifiers are added to Git.
- [ ] Backend validates all externally supplied assumptions.
- [ ] Monetary calculations and rounding are deterministic and tested.
- [ ] API contract and frontend consumer remain compatible.
- [ ] Autosave ordering and persisted state are correct under overlapping requests.
- [ ] Tests/builds pass; evidence is recorded.
- [ ] Docs and draw.io/Mermaid diagram source are updated when behavior or architecture changes.
- [ ] No unintended network exposure, logging or persistence side effects are introduced.

## Operational cautions

- Database volume contains the saved assumptions; deleting it destroys persisted data.
- CSV exports contain household financial data and should be stored/shared securely.
- Do not use `docker compose down -v` unless database deletion is intentional.
- Do not treat the seed defaults as the user's actual financial profile.
