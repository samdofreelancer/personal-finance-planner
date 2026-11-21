# Specification: Browser End-to-End Automation

**Status:** Accepted maintenance scope
**Rationale:** The application has unit/component tests and backend API integration tests, but no automated browser coverage of complete user journeys.

## Goal

Exercise the deployed frontend, reverse proxy, REST API, backend services and PostgreSQL persistence through real Chromium browser interactions. E2E tests must use synthetic data and must never read, seed, mutate or delete the developer's normal planner database.

## Scope

1. Start a dedicated Docker Compose project for E2E with distinct container names, loopback-only ports, network and PostgreSQL named volume.
2. Seed synthetic default assumptions and sample loan schedules before each test through the application's existing seed endpoint.
3. Automate browser journeys for dashboard rendering, expense schedule creation/effective-month persistence, loan schedule creation/persistence and CSV download.
4. Capture Playwright traces, screenshots and video on failure; produce an HTML report.
5. Run the same isolated E2E suite locally and in GitHub Actions.

## Acceptance criteria

- **E2E-01 Isolation:** Running E2E does not connect to the normal Compose project or its PostgreSQL volume. No command uses `down -v`.
- **E2E-02 Dashboard:** Chromium can load the app and observe 50 forecast months and five annual rows.
- **E2E-03 Expenses:** A user can add a category, define multiple effective-month amounts, save, reload and observe the persisted breakdown and effective values.
- **E2E-04 Loans:** A user can add a loan and dated repayment, save, reload and observe persisted values.
- **E2E-05 CSV:** Export initiates a download with the expected filename and forecast data.
- **E2E-06 Diagnostics:** Failed tests retain trace, screenshot and video; HTML report is generated.
- **E2E-07 CI:** GitHub Actions runs E2E independently of unit tests and uploads the report/results even on failure.
- **E2E-08 Repeatability:** Tests run sequentially and reset only the isolated E2E database before each scenario.

## Out of scope

- Authentication and multi-user authorization (not implemented by the product).
- Production data migration, destructive testing against the user's local database, cross-browser matrix and visual snapshot baselines.
- Replacing backend business-rule tests or frontend component tests with browser tests.
