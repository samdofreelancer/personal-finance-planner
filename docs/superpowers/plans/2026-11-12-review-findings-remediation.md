# System Review Findings Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the in-scope system review findings F-01, F-02, F-03, F-04, F-05, F-07, F-09 and F-10 while preserving the single-household local-first product.

**Architecture:** Keep a modular monolith. The Vue SPA talks to the Spring Boot API through the Nginx reverse proxy. The backend owns the authoritative forecast calculation; the forecast engine remains pure and independently testable. PostgreSQL stores the persisted assumptions, while temporary what-if requests are explicitly non-persisting. Deployment defaults expose only loopback ports.

**Tech Stack:** Vue 3, Vite, Java 21, Spring Boot 3.3.5, Maven, JUnit 5, Node.js built-in test runner, PostgreSQL 16, Docker Compose, Flyway.

**Spec:** `specs/001-review-findings-remediation/spec.md`

## Global Constraints

- Application remains a single-household local application; authentication, multi-tenancy, cloud hosting, and multi-user access are not introduced here.
- Existing forecast rules remain authoritative unless a discrepancy is discovered and explicitly resolved.
- Financial values remain whole VND at API/persistence boundaries unless existing contracts require otherwise; intermediate calculations use decimal-safe precision.
- Exact autosave concurrency mechanism is an implementation decision if observable consistency requirements are met.
- Exact Forecast Engine class decomposition is an implementation decision; avoid unnecessary abstraction.
- Exact simulation API path is an implementation decision; its contract must distinguish persisted forecast from non-persisting simulation.
- Java backend MUST be the authoritative production forecast calculation path.
- Backend monetary calculations MUST use decimal-safe arithmetic end-to-end; binary floating-point types MUST NOT be used for monetary values or intermediate monetary calculations.
- Default deployment MUST bind published application ports to loopback interfaces only and MUST NOT publish the PostgreSQL port to the host.
- Scenario creation, editing, comparison, versioning, and scenario-specific persistence are deferred (F-06).
- Multiple households, user accounts, tenant ownership, or assumptions ownership identity are deferred (F-08).

## Review Focus

1. **Rounding ties and cumulative rounding:** values ending in exactly .5 VND, compounded annual raises, and January multiplier must consistently use HALF_UP and preserve baseline forecast fixtures.
2. **Large values and overflow:** the largest accepted inputs, cumulative savings, annual aggregation, and intermediate sums must reject or fail safely rather than wrap a Java long or exceed the frontend's safe integer range.
3. **Partial or missing raise data:** null/missing raises maps and missing individual years must produce a controlled validation result or documented defaults, never an unhandled NullPointerException.
4. **Out-of-order and failed autosaves:** a slow earlier PUT, a later PUT, a failed first write, and a reset/seed operation must leave the newest successful user intent persisted.
5. **Existing database adoption:** a non-empty database without Flyway history must be backed up and explicitly baselined; fresh initialization must create the same schema without losing saved assumptions.

---

## File Structure

- `backend/src/main/java/com/finance/planner/config/WebConfig.java`: explicit CORS origin allowlist.
- `backend/src/main/resources/application.properties`: externalized database credentials, CORS configuration, Flyway and Hibernate schema mode.
- `docker-compose.yml`, `.env.example`, `.gitignore`, `README.md`: local-only bindings and safe local setup.
- `frontend/src/utils/serializedQueue.js`: small reusable serialized async-write queue.
- `frontend/src/App.vue`: route all assumption mutations through the queue; maintain accurate save status.
- `backend/src/main/java/com/finance/planner/service/ForecastService.java`: deterministic BigDecimal calculation and checked monetary arithmetic.
- `backend/src/main/java/com/finance/planner/service/ValidationService.java`: documented amount/rate bounds and safe null validation.
- `backend/src/test/java/com/finance/planner/service/`: focused backend unit tests for validation and forecast behavior.
- `frontend/src/utils/validation.js`: keep client bounds aligned with backend.
- `test/fixtures/legacy-forecast.mjs`, `test/forecast.test.mjs`: retain the legacy JavaScript calculator as a test-only oracle.
- `backend/src/main/resources/db/migration/V1__create_assumptions.sql`: initial schema for fresh databases.
- `backend/src/main/java/com/finance/planner/service/ForecastService.java` and focused policy classes: cohesive calculation responsibilities.
- `backend/src/main/java/com/finance/planner/controller/ForecastController.java`, `frontend/src/services/api.js`, `frontend/src/App.vue`: saved forecast vs simulation contract.
- `specs/001-review-findings-remediation/implementation-status.md`: finding-to-task-to-evidence traceability.

---

## Task 1: Secure the default local deployment (F-01)

**Files:**
- Modify: `docker-compose.yml`
- Modify: `backend/src/main/java/com/finance/planner/config/WebConfig.java`
- Modify: `backend/src/main/resources/application.properties`
- Create: `.env.example`
- Modify: `.gitignore`
- Modify: `README.md`
- Test: `backend/src/test/java/com/finance/planner/config/WebConfigTest.java`

**Interfaces:**
- CORS origins are configured by `app.cors.allowed-origins` and injected as a string array.
- Compose database password is required from `POSTGRES_PASSWORD`; the same value is passed to Spring as `SPRING_DATASOURCE_PASSWORD`.

- [ ] **Step 1: Write failing CORS tests**
  - Test that `http://localhost` is accepted.
  - Test that `http://localhost:5173` is accepted for Vite development.
  - Test that `https://untrusted.example` receives no allow-origin response header.
- [ ] **Step 2: Run the focused test and verify RED**
  - Run: `cd backend && mvn -Dtest=WebConfigTest test`
  - Expected: tests fail because wildcard CORS currently accepts the untrusted origin.
- [ ] **Step 3: Implement the explicit origin allowlist**
  - Replace `allowedOriginPatterns("*")` with `allowedOrigins(allowedOrigins)`.
  - Configure default origins for localhost, 127.0.0.1, and Vite localhost development only.
- [ ] **Step 4: Harden Compose defaults**
  - Bind frontend and backend host ports to `127.0.0.1`.
  - Remove the PostgreSQL host port mapping.
  - Require `POSTGRES_PASSWORD`; do not commit a working password.
  - Add `.env.example` with a placeholder that must be replaced; ignore `.env`.
  - Remove the weak datasource password fallback from `application.properties`.
- [ ] **Step 5: Update local setup documentation**
  - Document copying `.env.example` to `.env`, setting a unique password, and using localhost URLs.
  - Document that network exposure requires an explicit operator change to port bindings and CORS allowlist.
- [ ] **Step 6: Verify**
  - Run: `cd backend && mvn -Dtest=WebConfigTest test`
  - Run: `docker compose config` with a temporary local `.env` value.
  - Inspect effective port mappings: frontend/backend must use 127.0.0.1 and db must have no host port.
  - Run: `cd frontend && npm run build`
- [ ] **Step 7: Commit**
  - Commit message: `fix(security): restrict local deployment exposure`

## Task 2: Serialize assumption writes (F-02)

**Files:**
- Create: `frontend/src/utils/serializedQueue.js`
- Create: `test/serialized-queue.test.mjs`
- Modify: `frontend/src/App.vue`

**Interfaces:**
- Export `createSerializedQueue(): (operation: () => Promise<T>) => Promise<T>`.
- The returned enqueue function preserves invocation order and continues after a rejected operation.
- All assumption writes, including `saveAssumptions` and `seedDefaults`, use the same queue instance.

- [ ] **Step 1: Write failing queue tests**
  - Test that operation B does not start before slower operation A finishes.
  - Test that B still runs after A rejects.
  - Test that each caller receives its own operation result/rejection.
- [ ] **Step 2: Run the focused test and verify RED**
  - Run: `node --test test/serialized-queue.test.mjs`
  - Expected: module/function is missing.
- [ ] **Step 3: Implement the minimal serialized queue**
  - Chain each operation from the previous settled tail.
  - Keep a rejection-swallowed internal tail so one failed write does not poison later writes.
  - Return the original operation promise to its caller.
- [ ] **Step 4: Route every persistence mutation through the queue**
  - Use the same queue for debounced saves, explicit saves, and reset/seed.
  - Keep `latestRequest` checks for stale UI responses; they do not replace write serialization.
  - Only show saved status after the queued write succeeds.
- [ ] **Step 5: Verify**
  - Run: `node --test test/serialized-queue.test.mjs test/forecast.test.mjs`
  - Run: `cd frontend && npm run build`
  - Manually inspect the sequence: older slow save followed by newer save; reload must show newer values.
- [ ] **Step 6: Commit**
  - Commit message: `fix(frontend): serialize assumption persistence`

## Task 3: Make backend monetary calculations deterministic (F-03)

**Files:**
- Modify: `backend/src/main/java/com/finance/planner/service/ForecastService.java`
- Modify: `backend/src/main/java/com/finance/planner/service/ValidationService.java`
- Modify: `frontend/src/utils/validation.js`
- Create: `backend/src/test/java/com/finance/planner/service/ForecastServiceTest.java`
- Create: `backend/src/test/java/com/finance/planner/service/ValidationServiceTest.java`

**Interfaces:**
- Define one documented maximum supported monetary input in VND and apply it consistently to every monetary field, including optional payoff interest.
- Use `100,000,000,000 VND` as the inclusive maximum for each monetary input; this keeps the worst supported 50-month projection within JavaScript's safe-integer range under the current maximum rate and January multiplier.
- Keep whole-VND API values as `Long`; use `BigDecimal` for all salary/rate multiplications and `RoundingMode.HALF_UP` at the whole-VND boundary.
- Use checked arithmetic for sums/subtractions so overflow is never silently wrapped.

- [ ] **Step 1: Write failing backend tests**
  - Baseline fixture: 50 months, annual years 2026–2030, 2026 partial.
  - Baseline expected values are calculated from the synthetic sample fixture; keep personal household values out of committed tests and planning documents.
  - Test an exact .5 VND salary result rounds HALF_UP.
  - Test all current debt/provisional-interest and shortfall behavior.
  - Test a monetary input above the supported maximum is rejected.
  - Test null raises map and missing raise year are rejected predictably.
- [ ] **Step 2: Run focused tests and verify RED**
  - Run: `cd backend && mvn -Dtest=ForecastServiceTest,ValidationServiceTest test`
  - Expected: tests fail because the test classes/expected contracts are not implemented.
- [ ] **Step 3: Implement BigDecimal salary calculation**
  - Replace `double` factor, `doubleValue()`, and `round(double)` with exact BigDecimal multiplication and `setScale(0, HALF_UP).longValueExact()`.
  - Preserve April raise start, cumulative raises, and January multiplier semantics.
- [ ] **Step 4: Implement supported bounds and checked arithmetic**
  - Validate every money input, including optional payoff interest, against the same maximum.
  - Validate a null raises map and missing years without throwing NullPointerException.
  - Use checked addition/subtraction for monthly totals, loan payment, cumulative savings, and annual aggregation.
  - Mirror the same maximum in frontend validation.
- [ ] **Step 5: Verify**
  - Run: `cd backend && mvn test`
  - Run: `node --test test/forecast.test.mjs test/serialized-queue.test.mjs`
  - Run: `cd frontend && npm run build`
  - Search backend production sources for `double` or `float` in monetary calculation paths; expected: no matches.
- [ ] **Step 6: Commit**
  - Commit message: `fix(forecast): use deterministic monetary arithmetic`

## Task 4: Make Java the only production forecast path (F-04)

**Files:**
- Move: `forecast.mjs` → `test/fixtures/legacy-forecast.mjs`
- Modify: `test/forecast.test.mjs`
- Delete: `app.mjs`
- Delete: `index.html`
- Delete: `styles.css`
- Delete: `serve.py`
- Modify: `README.md`
- Backend tests: `backend/src/test/java/com/finance/planner/service/ForecastServiceTest.java`

**Interfaces:**
- The legacy JavaScript implementation remains available only to test the historical baseline/oracle.
- The supported production app is the Vue 3 frontend plus Spring Boot API documented by README and Docker Compose.

- [ ] **Step 1: Confirm all legacy entry points and references**
  - Search repository references to `app.mjs`, root `index.html`, `styles.css`, `serve.py`, and `forecast.mjs`.
  - Confirm README and Docker Compose use only `frontend/` and `backend/`.
- [ ] **Step 2: Move the legacy calculator under test fixtures**
  - Move `forecast.mjs` to `test/fixtures/legacy-forecast.mjs`.
  - Update the existing Node test import without changing its expected assertions.
- [ ] **Step 3: Retire the standalone legacy app entry point**
  - Delete the old root static UI and its Python serving entry point.
  - Update README to identify the Vue/Spring Boot application as the only supported runtime.
- [ ] **Step 4: Verify**
  - Run: `node --test test/forecast.test.mjs test/serialized-queue.test.mjs`
  - Run: `cd backend && mvn test`
  - Run: `cd frontend && npm run build`
  - Search production paths to confirm no runtime imports the test fixture.
- [ ] **Step 5: Commit**
  - Commit message: `refactor(app): retire legacy standalone forecast runtime`

## Task 5: Give the forecast engine focused policy boundaries (F-05)

**Files:**
- Modify: `backend/src/main/java/com/finance/planner/service/ForecastService.java`
- Create only cohesive policy classes under `backend/src/main/java/com/finance/planner/service/forecast/` when focused tests demonstrate a useful boundary.
- Test: `backend/src/test/java/com/finance/planner/service/forecast/`

**Interfaces:**
- Preserve `ForecastService.buildForecast(AssumptionDTO): ForecastResponse` as the application-facing entry point.
- Keep calculation policies pure: no Spring, database, HTTP, or filesystem dependencies.

- [ ] **Step 1: Add focused tests for income, expense phase selection, debt payoff, and annual aggregation rules currently embedded in ForecastService.**
- [ ] **Step 2: Run the focused tests and verify they fail for the missing policy seams.**
- [ ] **Step 3: Extract only cohesive policies with explicit input/output contracts; keep orchestration in ForecastService.**
- [ ] **Step 4: Run all backend tests and verify baseline fixtures remain unchanged.**
- [ ] **Step 5: Commit**
  - Commit message: `refactor(forecast): isolate calculation policies`

## Task 6: Adopt Flyway without losing existing assumptions (F-07)

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.properties`
- Create: `backend/src/main/resources/db/migration/V1__create_assumptions.sql`
- Modify: `README.md`
- Create: migration integration tests under `backend/src/test/java/com/finance/planner/migration/`

**Interfaces:**
- Fresh PostgreSQL databases are initialized by versioned SQL migrations.
- Hibernate uses schema validation, not schema mutation.
- Existing non-empty databases require backup and a documented baseline-adoption path; do not run destructive schema changes automatically.

- [ ] **Step 1: Write migration integration tests**
  - Test fresh database migration creates the assumptions table and all current columns/default-compatible nullability.
  - Test existing schema adoption retains a seeded assumptions row.
  - Test Hibernate validation succeeds after migrations.
- [ ] **Step 2: Run focused migration tests and verify RED.**
- [ ] **Step 3: Add Flyway dependencies and V1 SQL matching the current Assumption entity schema.**
- [ ] **Step 4: Configure Flyway and set Hibernate schema mode to `validate`; document backup and explicit baseline steps for an existing database.**
- [ ] **Step 5: Run fresh-database and existing-database migration tests; verify saved assumptions remain unchanged.**
- [ ] **Step 6: Commit**
  - Commit message: `feat(database): manage schema with Flyway`

## Task 7: Separate saved forecast from temporary simulation (F-09)

**Files:**
- Modify: `backend/src/main/java/com/finance/planner/controller/ForecastController.java`
- Modify: `frontend/src/services/api.js`
- Modify: `frontend/src/App.vue`
- Test: backend controller/service tests and frontend API behavior tests.

**Interfaces:**
- `GET /api/forecast`: load persisted assumptions through AssumptionService and calculate the saved forecast.
- `POST /api/forecast/simulate`: accept an AssumptionDTO, validate it, calculate from request data, and perform no persistence writes.
- `GET /api/export/csv`: export the saved forecast.
- Frontend `getForecast()` has no DTO argument; `simulateForecast(dto)` accepts temporary assumptions.

- [ ] **Step 1: Write failing tests proving GET uses saved assumptions and POST simulation does not persist.**
- [ ] **Step 2: Run focused tests and verify RED.**
- [ ] **Step 3: Implement the saved forecast and simulation controller contracts.**
- [ ] **Step 4: Update frontend initial load to GET saved forecast, live preview to POST simulation, and post-save refresh to GET saved forecast.**
- [ ] **Step 5: Update CSV export to use the saved forecast after pending writes complete.**
- [ ] **Step 6: Verify saved assumptions A remain unchanged after simulation B; run backend tests and frontend build.**
- [ ] **Step 7: Commit**
  - Commit message: `feat(api): separate saved forecast and simulation`

## Task 8: Close review-to-delivery traceability (F-10)

**Files:**
- Create: `specs/001-review-findings-remediation/implementation-status.md`
- Modify: `specs/001-review-findings-remediation/spec.md` only for accepted clarifications/status and finding disposition.
- Create: `docs/superpowers/reviews/2026-11-12-review-findings-remediation.md`

- [ ] **Step 1: Create a status matrix mapping F-01–F-10 to FR IDs, plan task, implementation commit, and verification evidence.**
- [ ] **Step 2: Mark F-06 and F-08 deferred with their revisit triggers; do not create implementation tasks for them.**
- [ ] **Step 3: Record exact test/build/migration evidence for every completed task; leave unverified findings open.**
- [ ] **Step 4: Update the remediation spec status only after all in-scope findings are either verified complete or explicitly parked with a ruling.**
- [ ] **Step 5: Commit**
  - Commit message: `docs(review): record remediation traceability`

---

## Verification Gate

Before the remediation is considered complete:

- [ ] `node --test test/forecast.test.mjs test/serialized-queue.test.mjs` passes.
- [ ] `cd backend && mvn test` executes meaningful backend tests and passes.
- [ ] `cd frontend && npm run build` passes.
- [ ] `docker compose config` resolves with a local `.env`; ports and credential settings satisfy F-01.
- [ ] Fresh database migration and existing database adoption are verified without losing assumptions.
- [ ] Saved forecast and temporary simulation semantics are verified.
- [ ] Every in-scope finding has a linked verification record; F-06 and F-08 remain explicitly deferred.
