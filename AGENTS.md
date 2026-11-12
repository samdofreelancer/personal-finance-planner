# AGENTS.md

Instructions for coding agents (OpenCode, Copilot, Claude Code, Cursor, Codex) working in this repository.

## What this project is

Personal Finance Planner 2026–2030: a local-first web app for a single household. It is not a multi-tenant or internet-facing product.

- `frontend/` — Vue 3 + Vite; the production build is served by Nginx.
- `backend/` — Spring Boot 3 (Java 21), REST API under `/api`, PostgreSQL 16.
- `test/` — Node test-runner tests. `test/fixtures/legacy-forecast.mjs` is a historical oracle only; it is not production code.
- `docs/` — engineering documentation. Start at `docs/README.md`.
- `specs/` — SpecKit specifications, plans and tasks. `specs/001-review-findings-remediation/` tracks pending remediation work.
- `docker-compose.yml` — local stack: `db`, `backend`, `frontend`.

There is no authentication, multi-tenancy, bank sync, or production deployment target. Keep the system a local-first modular monolith unless a spec explicitly changes that decision (`docs/decisions/0001-local-first-modular-monolith.md`).

## Quick commands

| Task | Command |
|---|---|
| Run full stack | `docker compose up --build` |
| Stop, keep database | `docker compose down` |
| Stop, delete database | `docker compose down -v` (destructive) |
| Backend only | `cd backend && mvn spring-boot:run` |
| Frontend only | `cd frontend && npm install && npm run dev` |
| Backend tests | `cd backend && mvn test` |
| Frontend production build | `cd frontend && npm ci && npm run build` |
| Legacy oracle tests | `node --test test/forecast.test.mjs` |
| Verify everything locally | `node scripts/verify.mjs` |
| Smoke-test a running stack | `node scripts/smoke.mjs` |

Prerequisites: Docker + Docker Compose; Java 21 + Maven; Node.js 18+ (CI uses Node 20).

## Running and "deploying"

There is only a local deployment today. Do not invent production infrastructure.

1. `Copy-Item .env.example .env` and set a unique `POSTGRES_PASSWORD`.
2. `docker compose up --build`.
3. Open `http://localhost` (frontend), `http://localhost:8080` (backend API), `localhost:5432` (PostgreSQL).

Standalone backend requires `SPRING_DATASOURCE_PASSWORD` and a reachable PostgreSQL; see `backend/src/main/resources/application.properties`. Standalone frontend runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080`.

Ports 80, 8080 and 5432 are published on all interfaces by the current compose file. Do not expose this stack to an untrusted network (see F-01 in `specs/001-review-findings-remediation/spec.md`).

## Verification expectations

A change is not complete until all applicable checks pass:

1. `mvn -B test` in `backend/`.
2. `npm run build` in `frontend/` (run `npm ci` first in a clean checkout).
3. `node --test test/forecast.test.mjs`.
4. For runtime/API/UI changes: start the stack and run `node scripts/smoke.mjs`.
5. Manual UI check against `http://localhost` when the change affects the UI.

Use `node scripts/verify.mjs` for steps 1–3 in one command. Record the commands and results in the task/spec when the project workflow asks for verification evidence.

Important current limitation: **there are no backend test sources yet** (`backend/` has no `src/test/` tree), so `mvn test` compiles the application but runs zero tests. Do not claim Java forecast behavior is verified by that command. When changing forecast, validation, API or persistence behavior, add JUnit tests under `backend/src/test/java/` first. There are also no frontend unit tests or lint script configured.

CI lives in `.github/workflows/ci.yml` and runs the backend tests, frontend build and legacy oracle tests on pushes and pull requests.

## API and behavior rules

- The API contract is documented in `docs/04-api-contract.md`; business rules are in `docs/03-business-rules.md`.
- `GET /api/forecast` and `GET /api/export/csv` bind query parameters to `AssumptionDTO` and calculate from that DTO. They do **not** read the saved assumptions row (F-09). Do not silently change this contract.
- `PUT /api/assumptions` saves the single assumptions row. Validation lives in `ValidationService`; keep server-side validation authoritative.
- Do not change DTO field names, query parameter names, rounding, or forecast semantics without updating backend, frontend and docs together.
- Keep monetary calculations deterministic and covered by backend tests.

## Engineering workflow

1. Clarify behavior and acceptance criteria before implementing.
2. Update the relevant SpecKit specification and keep findings traceable to requirement IDs.
3. For new behavior, write or extend tests before or with the implementation.
4. Update `docs/02-architecture.md`, `docs/04-api-contract.md`, `docs/06-development-guide.md` and the Mermaid diagram when boundaries, flows or contracts change.
5. Run the verification commands above and record evidence.
6. Review security/privacy impact for changes touching deployment, logs, exports, persistence or API inputs.

## Safety rules

- Never commit `.env`, credentials, real financial data, tokens or personal identifiers. `.env` is git-ignored; `.env.example` is the only committed template.
- CSV exports contain household financial data; handle and store them accordingly.
- `docker compose down -v` deletes the PostgreSQL volume. Never run it without explicit intent.
- Do not add logging of salaries, expenses, loan details or exported rows.
- Do not re-add the deleted legacy root app (`app.mjs`, `forecast.mjs`, `index.html`, `styles.css`, `serve.py`); the supported app is Vue + Spring Boot.

## CodeGraph

The repository is indexed by CodeGraph (`.codegraph/`). Use it before broad grepping:

```powershell
codegraph explore "<question or symbol>"
codegraph callers <symbol>
codegraph impact <symbol>
codegraph sync          # after code changes
```

The index database is local-only; `.codegraph/.gitignore` keeps it out of Git.

## Documentation map

- `docs/README.md` — documentation index and as-is vs to-be rules.
- `docs/02-architecture.md` — current runtime and known gaps.
- `docs/03-business-rules.md` — forecast formulas and rounding.
- `docs/04-api-contract.md` — endpoints and DTO semantics.
- `docs/05-data-model.md` — persistence and sensitive data.
- `docs/06-development-guide.md` — setup, verification, checklist and operational cautions.
