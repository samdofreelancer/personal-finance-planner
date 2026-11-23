# E2E Package and Page Object Model Design

## Intent

Move browser automation into a standalone `e2e/` package so frontend unit-test dependencies and responsibilities remain independent. Organize browser scenarios as Playwright specs that use Page Objects for UI navigation, locators and interactions.

## Approved approach

Use an independent npm package under `e2e/`, with its own `package.json`, lockfile and Playwright configuration. Keep `frontend/` responsible for Vue/Vite and Vitest only. The E2E package owns `tests/`, `pages/`, `fixtures/`, its runner, browser reports and test results.

The existing isolated Docker Compose project remains the application-under-test environment. The runner continues to seed only its dedicated synthetic database, execute tests sequentially, preserve the E2E database volume, and stop containers without `down -v`.

## Structure and responsibilities

- `e2e/tests/*.spec.js`: business journeys and observable assertions.
- `e2e/pages/*.js`: page-specific locators and user interactions; shared navigation belongs in `BasePage`.
- `e2e/fixtures/test.js`: Playwright fixtures that provide Page Objects and seed synthetic data through the isolated API before each test.
- `e2e/playwright.config.js`: fixed isolated base URL, sequential execution and failure diagnostics.
- `e2e/scripts/run-e2e.mjs`: starts/waits for/stops the isolated Compose stack and invokes the local Playwright CLI.
- `e2e/package.json` and `e2e/package-lock.json`: Playwright-only dependency boundary and repeatable install.

## Goals and non-goals

Preserve all existing dashboard, expense schedule, loan schedule and CSV journeys while removing direct UI locator work from specs. Keep backend business rules and frontend component tests in their existing test layers. This change does not add product behavior, authentication, cross-browser coverage or visual snapshots.

## Validation

Run the E2E suite through `npm ci` and `npm run e2e` from `e2e/`, confirm its report/results are written under that package, and run the existing repository verification for backend/frontend tests, frontend build and Compose configuration. CI must install and execute E2E from `e2e/` and upload diagnostics from the new paths.
