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
./scripts/verify.ps1                     # backend tests + JaCoCo + PIT, frontend build, both Compose configs
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

`mvn verify` runs the test suite, creates the JaCoCo HTML/XML report and enforces the current bundle thresholds: at least 80% line coverage and 70% branch coverage. The repository verification script then runs PIT mutation testing with an 80% score threshold. Reports are written to `backend/target/site/jacoco/` and `backend/target/pit-reports/`; open each `index.html` for the HTML view. Thresholds are deliberately below the measured baseline so new code can improve coverage without blocking unrelated work.

Backend unit tests currently cover:

- `ForecastServiceTest`: 50-month/annual aggregation, debt principal and payoff boundaries, provisional payoff interest, child/rent transitions, salary raises and January multiplier, rounding, cash flow and negative savings.
- `ValidationServiceTest`: valid assumptions, zero/negative/missing monetary boundaries, multiplier lower/upper bounds, interest/payment boundaries, raise bounds and missing payoff-interest confirmation.

The backend currently has 29 automated tests (5 API integration, 10 forecast unit and 14 validation unit tests). The measured JaCoCo baseline is approximately 86.9% line and 77.9% branch coverage across the backend bundle.

Backend API integration tests:

- `PlannerApiIntegrationTest` boots the real Spring application, MVC controllers, services, Spring Data JPA and an isolated in-memory H2 database in PostgreSQL compatibility mode.
- It verifies seed/read, update/read persistence, rejection of invalid updates without changing stored data, the 50-month forecast response and CSV download headers/content.
- The test datasource uses `jdbc:h2:mem:` and `create-drop`; it must never point at the developer's or production PostgreSQL database.

GitHub Actions workflow `.github/workflows/ci.yml` runs `mvn verify` for the backend and `npm ci` plus `npm run build` for the frontend on pushes and pull requests. The CI database is in-memory; no PostgreSQL service or secrets are required.

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

   The scan script securely prompts for the token if `SONAR_TOKEN` is not already set, runs `mvn clean verify` first, then invokes the official SonarScanner CLI container against the local server. It analyzes backend Java and frontend Vue/JavaScript source and imports the JaCoCo XML report. The scanner waits up to five minutes for the Quality Gate and returns a failure if the gate fails.

4. Re-scan after changes with `./scripts/sonar-scan.ps1`. If tests and JaCoCo have already run, `./scripts/sonar-scan.ps1 -SkipTests` skips the Maven step. The project must exist in SonarQube before the first scan; create it once in the UI using the exact key above.
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
