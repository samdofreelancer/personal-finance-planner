# 06. Development Guide

## Prerequisites

- Docker Engine/Desktop and Docker Compose for the full local stack.
- For isolated development: Java 21 and Maven for backend; Node.js 20.19+ (or 22.12+) and npm for frontend.
- PostgreSQL 16 when running backend outside Docker.

## Database schema migrations

- Flyway is the only schema migration mechanism; Hibernate runs with `ddl-auto=validate`.
- A fresh empty database runs V1–V4. V1 creates the assumptions table and synthetic defaults; V2 creates loan tables and seeds an equivalent sample repayment schedule; V3 removes the retired single-loan columns; V4 converts fixed living/child/rent fields into flexible expense categories and effective-month amount changes.
- On an existing non-empty database without Flyway history, `baseline-on-migrate=true` baselines the existing schema at version 1, then runs V2–V4. The assumptions row, existing loan schedule and equivalent recurring expense amounts are retained.
- **Before the first application start after this change, take a PostgreSQL backup.** Do not delete the Compose database volume. If Flyway reports a failed migration, inspect the database and migration history before retrying; never repair by dropping tables or volumes.
- V1–V3 SQL was executed against isolated temporary PostgreSQL 16 databases for synthetic fresh defaults and a modified legacy fixture. On 2026-11-19, the live local Compose database was backed up to `backups/finance-planner-pre-deploy-20261119.dump` (custom-format archive verified with `pg_restore --list`), then Flyway successfully baselined it at V1 and applied V2/V3. Post-migration checks confirmed one assumptions row, one migrated loan with three repayment entries, and removal of all six retired loan columns. Frontend, assumptions, loans, 50-month/5-year forecast and CSV HTTP smoke checks passed. The PostgreSQL volume was preserved.
- On 2026-11-21, the live local Compose database was backed up to `backups/finance-planner-pre-v4-20261121.dump` (PostgreSQL custom-format archive; `pg_restore --list` succeeded; SHA-256: `3BA09863587E53E9DD2D15C2F719680A832E5282087B9E3E28D987BBCB4ED2C1`). The rebuilt backend applied Flyway V4 successfully on PostgreSQL 16.15. Verification confirmed three migrated expense categories with six effective-month changes, all legacy expense amounts/months preserved, and all six retired fixed-expense columns removed. Existing assumptions and loan data remained present (one assumptions row, one loan and three repayment entries). A temporary expense category was added, renamed, assigned a future effective-month amount, forecasted and exported to CSV through the live API, then deleted; the original assumptions snapshot was restored and compared exactly. The 50-month forecast and frontend HTML/JS/CSS assets returned successfully. Both backend and frontend Compose images were rebuilt and are running. The backup is ignored by Git and must not be committed. Never use `docker compose down -v` for this migration.

## Run with Docker Compose

```powershell
Copy-Item .env.example .env
# Set a unique local POSTGRES_PASSWORD in .env before startup.
docker compose up --build
```

### Image build caching

`docker compose build` builds both images in stages. The backend keeps Maven's local repository in a BuildKit cache mount (`/root/.m2`) and only re-resolves dependencies when `pom.xml` changes; the frontend installs with `npm ci` against `package-lock.json` and keeps npm's cache in a mount (`/root/.npm`). The image build skips backend test compilation and execution (`-Dmaven.test.skip=true`) because `mvn verify` and CI own the test run.

The first build on a machine downloads the whole Maven dependency tree and can take several minutes; later builds reuse the cache mount. An interrupted build keeps the artifacts it already downloaded.

Production Compose publishes only frontend/Nginx on `127.0.0.1:80`; remote access is provided by Tailscale Serve over the private tailnet. The backend (8080) and PostgreSQL (5432) are reachable only inside the Docker network. See [Remote access with Tailscale](11-remote-access-tailscale.md) for setup and security details. The development overlay intentionally publishes Vite (5173) and backend (8080) for local developer workflows; do not use that overlay as the remote-access stack.

Stop containers but keep persisted data:

```powershell
docker compose down
```

Delete containers and the database volume (destructive):

```powershell
docker compose down -v
```

## Run with a Docker Engine inside WSL2 (Windows)

Docker Desktop is optional. When `docker` points at a Docker Engine running inside a WSL2 distribution, a few constraints apply:

- A Compose file that bind-mounts host directories must be started from inside the WSL distribution. A Linux daemon cannot resolve Windows paths such as `C:\...`; the repository has to be reachable as `/mnt/c/...`.
- The development overlay (`docker-compose.dev.yml`) bind-mounts `./backend` and `./frontend`, so start it from WSL. A small Windows wrapper that maps the current folder and runs Compose inside WSL keeps the command usable from the repository root:

  ```powershell
  wsl-docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
  ```

- In the production Compose stack, the frontend is bound to Windows loopback at `http://127.0.0.1`; backend and PostgreSQL are not published. Tailscale Serve proxies private tailnet traffic to that loopback endpoint. The development overlay additionally publishes Vite (5173) and backend (8080) for local development only.
- After `wsl --shutdown` the engine is offline until the distribution boots again; start it explicitly with `wsl -d Ubuntu -u root -e systemctl start docker ssh`.

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
./scripts/verify.ps1                     # backend tests + JaCoCo + PIT, frontend tests + coverage + build, both Compose configs
./scripts/verify.ps1 -DockerBuild        # above + production image build
./scripts/verify.ps1 -SmokeTest          # above + HTTP checks against an already-running production-like stack
./scripts/verify.ps1 -SmokeTest -DevStack # use frontend port 5173 for an already-running dev stack
```

The script is intentionally non-destructive: it does not start/stop containers, seed/reset assumptions or remove database volumes. Start the stack separately before a smoke test. A smoke test checks that the frontend responds and `GET /api/assumptions` returns a response; it is not a full end-to-end financial calculation test.

Backend build/tests:

```powershell
cd backend
mvn verify
```

`mvn verify` runs the backend test suite, creates the JaCoCo HTML/XML report and enforces the current bundle thresholds: at least 80% line coverage and 70% branch coverage. The repository verification script then runs PIT mutation testing with an 80% score threshold. The latest run generated 72 mutations and killed 68 (94% mutation score). Reports are written to `backend/target/site/jacoco/` and `backend/target/pit-reports/`; open each `index.html` for the HTML view.

Backend unit tests currently cover:

- `ForecastServiceTest`: 50-month/annual aggregation, per-loan principal caps, repayments outside the forecast window, user-defined expense effective-month boundaries, salary raises and January multiplier, rounding, cash flow and negative savings.
- `ValidationServiceTest`: valid assumptions, expense names/schedules/duplicate effective months and amounts, zero/negative/missing monetary boundaries, multiplier lower/upper bounds and raise bounds.
- `ExpenseMigrationTest`: V1-to-V4 fixture migration preserves every legacy expense amount and removes the retired fixed expense columns.
- Loan schedule API integration tests: multiple loans, multi-entry monthly aggregation, validation, update/delete identity preservation, CSV parity and reset sample data.

The backend currently has 41 automated tests (11 API integration, 12 forecast unit, 16 validation unit, 1 assumptions service unit and 1 migration test). Coverage gates are enforced by `mvn verify`; see the generated JaCoCo report for the current measurement.

Frontend tests use Vitest, Vue Test Utils and jsdom. `npm test` runs the component/service/utility tests, enforces at least 80% line and statement coverage, 70% function coverage and 75% branch coverage, then writes `frontend/coverage/lcov.info` for SonarQube. The LCOV normalizer prefixes source paths with `frontend/` so the report resolves from the repository root. The report directory is generated and ignored by Git.

The frontend currently has 58 automated tests covering currency input/formatting, dynamic expense schedule editing and validation, loan schedule editing/dirty-state feedback, API requests and error handling, forecast presentation components and the application lifecycle/save/reset/export flows.

Latest local SonarQube analysis after importing both reports: 90.3% overall coverage, 92.4% line coverage and 87.5% branch coverage. The Quality Gate passed.

Backend API integration tests:

- `PlannerApiIntegrationTest` boots the real Spring application, MVC controllers, services, Spring Data JPA and an isolated in-memory H2 database in PostgreSQL compatibility mode.
- It verifies seed/read, update/read persistence, rejection of invalid updates without changing stored data, the 50-month forecast response and CSV download headers/content.
- The test datasource uses `jdbc:h2:mem:` and `create-drop`; it must never point at the developer's or production PostgreSQL database.

GitHub Actions workflow `.github/workflows/ci.yml` runs `mvn verify` for the backend and `npm ci`, `npm test` and `npm run build` for the frontend on pushes and pull requests. The CI database is in-memory; no PostgreSQL service or secrets are required.

### Browser end-to-end tests (Playwright)

Playwright exercises the real Vue UI through Nginx, Spring Boot and PostgreSQL. It is intentionally a separate test layer from Vitest component tests and H2 backend integration tests.

Run from `frontend/`:

```powershell
npm ci
npx playwright install chromium   # first run on a machine
npm run e2e                       # starts isolated Compose stack, runs tests, then stops containers
```

The runner uses `docker-compose.e2e.yml` and the fixed Compose project `personal-finance-planner-e2e`: frontend `http://localhost:18088`, backend `http://localhost:18080`, PostgreSQL `localhost:15432`. It supplies a stable, local-only E2E database password and does not load the repository `.env`; this is required because the isolated E2E database volume persists between runs. The project has distinct container names, network and named database volume; it never attaches to `finance_planner_db_data` or the normal application containers. Each test resets only this synthetic E2E database through `POST /api/assumptions/seed`.

The runner stops E2E containers with `docker compose down` and deliberately preserves the E2E-only volume for reuse. It never runs `down -v`. Do not manually change the E2E project to point at the normal planner database. The suite is sequential because each scenario seeds a shared isolated database.

Playwright writes `frontend/playwright-report/` (HTML) and `frontend/test-results/` (traces, screenshots and video on failure). GitHub Actions runs the same `npm run e2e` command in a separate job and uploads diagnostics as `playwright-e2e-report` for 14 days.

The initial browser journeys cover dashboard rendering and the 50-month/5-year forecast, user-defined expense schedules and effective-month persistence, loan/repayment persistence, and CSV download. This does not replace business-rule tests or claim cross-browser/visual-regression coverage.

The backend CI job also runs PIT and uploads the JaCoCo (`backend-jacoco-report`) and PIT (`backend-pit-report`) reports as artifacts for 14 days. SonarQube is intentionally local-only; CI does not depend on a developer machine or a locally hosted server.

## Mutation testing

PIT targets `ForecastService` and `ValidationService` plus their service unit tests. Run it from `backend/`:

```powershell
mvn org.pitest:pitest-maven:mutationCoverage
```

Open `backend/target/pit-reports/index.html` to inspect killed, survived and uncovered mutants. Mutation testing is a diagnostic quality signal, not a replacement for requirement-based tests; review surviving mutants before changing assertions or setting a mutation-score gate.

The initial focused run against `ForecastService` and `ValidationService` killed all 108 generated mutants (100%). PIT enforces an 80% mutation-score minimum for those two business-critical services; expand the target set only when corresponding tests exist.

## Local SonarQube Community Build

SonarQube runs in a separate Compose project and uses its own PostgreSQL database and named volumes. It does not reuse or modify the planner's application database. The web port is bound to loopback only (`http://localhost:9000`).

1. Add a private `SONAR_DB_PASSWORD` value to `.env` (the local setup may generate one); never commit `.env`.
2. Start the local analysis server:

   ```powershell
   docker compose -f docker-compose.sonar.yml up -d
   docker compose -f docker-compose.sonar.yml ps
   ```

3. Open `http://localhost:9000`, sign in initially with `admin` / `admin`, change the password, and create a project with key `personal-finance-planner`. Then open **My Account → Security**, generate a **Project Analysis Token**, and scope it to this project. Do not save the token in `.env`, source control or shell history.

   ```powershell
   ./scripts/sonar-scan.ps1
   ```

   The scan script securely prompts for the token if `SONAR_TOKEN` is not already set, runs `mvn clean verify` and frontend `npm ci`/`npm test` first, then invokes the official SonarScanner CLI container against the local server. It analyzes backend Java and frontend Vue/JavaScript source and imports both JaCoCo XML and frontend LCOV reports. The scanner waits up to five minutes for the Quality Gate and returns a failure if the gate fails.

4. Re-scan after changes with `./scripts/sonar-scan.ps1`. If backend and frontend tests/coverage have already run, `./scripts/sonar-scan.ps1 -SkipTests` skips both test steps. The project must exist in SonarQube before the first scan; create it once in the UI using the exact key above.
5. Stop the server while preserving its database and analysis history:

   ```powershell
   docker compose -f docker-compose.sonar.yml down
   ```

Do not use `down -v` for routine shutdown; it deletes SonarQube's separate history/database volumes. Elasticsearch requires host `vm.max_map_count` of at least `524288`, `fs.file-max` of at least `131072`, at least `131072` open files and `8192` threads for the SonarQube process. The Compose service sets the container `nofile`/`nproc` limits; Docker Desktop may need the corresponding WSL2 kernel settings for host-level values.

Keep these tests focused on Java domain behavior. Do not add a parallel JavaScript forecast implementation as a test oracle.

Frontend production build:

```powershell
cd frontend
npm ci
npm test
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
