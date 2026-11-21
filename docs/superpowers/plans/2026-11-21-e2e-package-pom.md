# E2E Package and Page Object Model Implementation Plan

> **For agentic workers:** Execute this plan task-by-task using the current Chat On Steroids session. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move Playwright into an independently installable `e2e/` package and make browser specs use Page Objects for all UI interactions.

**Architecture:** `frontend/` owns Vitest and Vue build only. `e2e/` owns Playwright dependencies, configuration, fixtures, page objects, business journey specs, runner and diagnostics. The existing isolated Docker Compose stack and synthetic database lifecycle remain the system under test.

**Tech Stack:** Node.js 22, npm, Playwright Test 1.65, JavaScript ES modules, Docker Compose.

**Spec:** `docs/superpowers/specs/2026-11-21-e2e-package-pom-design.md`; acceptance requirements in `specs/003-playwright-e2e/spec.md`.

## Global Constraints

- Keep E2E bound to `http://127.0.0.1:18088`; do not allow environment overrides to redirect it to a normal planner instance.
- Keep E2E Compose project `personal-finance-planner-e2e`, isolated database credentials, volume and ports.
- Tests remain sequential (`fullyParallel: false`, `workers: 1`) because each scenario seeds shared synthetic state.
- Preserve trace, screenshot, video and HTML diagnostics on failure.
- Never use `docker compose down -v`; preserve all unrelated working-tree changes and local data.
- Keep Playwright out of frontend dependencies and frontend test scripts.

## Review Focus

- Missing E2E package install: runner should fail with a clear prerequisite message before starting containers.
- Interrupted runner or Compose startup failure: diagnostics and cleanup must retain the existing behavior and preserve the database volume.
- Environment variables attempting to override the target URL: browser tests must still use the isolated E2E endpoint.
- Failed browser scenario: Playwright must retain traces, screenshots and video under `e2e/`.
- Parallel execution: config must keep one worker so seed/reset operations cannot race.

---

### Task 1: Create the independent Playwright package and migrate the runner

**Files:**
- Create: `e2e/package.json`
- Create: `e2e/package-lock.json`
- Create: `e2e/scripts/run-e2e.mjs` (move and adapt `scripts/run-e2e.mjs`)
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Modify: `.gitignore`
- Delete: `scripts/run-e2e.mjs`

**Interfaces:**
- E2E package script: `npm run e2e` invokes `node scripts/run-e2e.mjs` from `e2e/`.
- Runner resolves repository root as the parent of `e2e/`, invokes Playwright CLI from `e2e/node_modules`, and uses `e2e/` as Playwright working directory.
- Frontend package scripts retain only `dev`, `build`, `preview` and `test`.

- [ ] Create package manifest with only `@playwright/test` as a direct development dependency and generate its lockfile.
- [ ] Remove Playwright from frontend manifest and regenerate the frontend lockfile without changing other dependency versions.
- [ ] Move runner into `e2e/scripts`; adapt all paths to the package location and add a clear check for the local Playwright CLI before Compose startup.
- [ ] Move generated report/result ignore rules to the E2E package boundary and keep frontend ignore rules for frontend outputs only.
- [ ] Run `npm ci --ignore-scripts --offline` in `frontend/` and `e2e/` to prove both lockfiles install independently when cache permits.

### Task 2: Add fixtures and Page Objects, then split browser journeys into specs

**Files:**
- Create: `e2e/playwright.config.js`
- Create: `e2e/fixtures/test.js`
- Create: `e2e/pages/BasePage.js`
- Create: `e2e/pages/DashboardPage.js`
- Create: `e2e/pages/AssumptionsPage.js`
- Create: `e2e/pages/LoansPage.js`
- Create: `e2e/tests/dashboard.spec.js`
- Create: `e2e/tests/expense-schedules.spec.js`
- Create: `e2e/tests/loans.spec.js`
- Create: `e2e/tests/csv-export.spec.js`
- Delete: `frontend/playwright.config.js`
- Delete: `frontend/e2e/planner.e2e.js`

**Interfaces:**
- `BasePage(page)` exposes `goto(path = '/')`.
- `DashboardPage(page)` exposes dashboard heading, monthly/annual forecast locators and CSV export/download behavior.
- `AssumptionsPage(page)` exposes `open()`, `addExpenseSchedule({ name, initialMonth, initialAmount, nextMonth, nextAmount })`, `save()`, `reload()`, and observable save/name/forecast locators.
- `LoansPage(page)` exposes `open()`, `createLoanWithRepayment({ name, principal, repaymentDate, repaymentPrincipal, repaymentInterest })`, `save()`, `reload()`, and persisted loan locators.
- Fixture module exports `{ test, expect }`; its `test` extends Playwright with page object fixtures and seeds only the isolated API before each test.
- Specs import only `{ test, expect }` from `../fixtures/test.js`; they do not use raw `page`, `locator`, or direct browser interaction.

- [ ] Create the four spec files first with the existing scenario names and observable expectations; imports reference the intended fixture/page interfaces.
- [ ] Run Playwright discovery to confirm the new specs fail because the new fixture/page modules do not exist yet.
- [ ] Implement the fixture and page objects with locators and UI actions encapsulated in page classes.
- [ ] Add Playwright config with fixed E2E base URL, sequential execution, CI retry/forbidOnly behavior and reports under `e2e/`.
- [ ] Run the suite against the isolated stack and verify the four journeys pass.

### Task 3: Update CI, docs and architecture diagram

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `README.md`
- Modify: `docs/06-development-guide.md`
- Modify: `docs/02-architecture.md`
- Modify: `docs/diagrams/system-architecture.mmd`
- Modify: `docs/README.md`

**Interfaces:**
- CI E2E job uses `e2e/package-lock.json`, installs Playwright browser, runs `npm run e2e` from `e2e/`, and uploads `e2e/playwright-report/` plus `e2e/test-results/`.
- Local instructions run `npm ci`, `npx playwright install chromium` and `npm run e2e` from `e2e/`.

- [ ] Update E2E CI working directory, cache key, install, browser install, execution and artifact paths.
- [ ] Update README and development guide with root-level `e2e/` commands and new report locations.
- [ ] Document E2E package boundaries in architecture and docs index.
- [ ] Add the test automation flow to the system architecture diagram, showing specs -> fixtures/Page Objects -> Playwright/Chromium -> isolated Compose app and E2E PostgreSQL.
- [ ] Run `docker compose -f docker-compose.yml -f docker-compose.e2e.yml config` and the repository verification script.

### Task 4: Final verification and review

**Files:**
- Review all changed files and generated lockfiles.

- [ ] Run the full E2E suite from `e2e/` and record the actual scenario count and result.
- [ ] Confirm Playwright report and test result directories are under `e2e/`.
- [ ] Run `./scripts/verify.ps1` and record backend/frontend test counts, coverage/PIT outcome, frontend build and Compose validation.
- [ ] Review the complete Git diff and ensure `scripts/start-stack.ps1`, database backups and unrelated files are untouched.
