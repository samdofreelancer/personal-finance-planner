# Tasks: Personal Finance Planner

**Status:** Complete.

## Phase 1 — Confirm requirements

- [x] Use the latest workbook values as editable defaults (2.5× multiplier, 3,683,193 VND payoff interest); disclose that payoff interest is not independently verified.
- [x] Apply nearest-VND rounding for raised salary and integer VND for all monetary calculations.
- [x] Use local persistence and CSV export without cloud sync.

## Phase 2 — Build the forecast domain

- [x] Define editable assumptions and monthly/annual forecast result models.
- [x] Implement salary, expense, loan, monthly net cash flow, and cumulative savings calculations.
- [x] Represent missing payoff interest explicitly and propagate provisional status.
- [x] Add unit tests for calculation requirements.

## Phase 3 — Build the local interface

- [x] Implement the Vietnamese assumptions form with validation and reset.
- [x] Implement monthly forecast table and annual summary with partial-year indication.
- [x] Implement the annual net-cash-flow chart with labeled years and zero baseline.
- [x] Add versioned local persistence and CSV export.
- [x] Add responsive layout and keyboard/accessibility support.

## Phase 4 — Verify and deliver

- [x] Verify 50 monthly rows, annual values, and chart/table consistency with automated forecast tests.
- [x] Verify reload persistence and reset behavior in a browser.
- [x] Verify CSV export includes the displayed annual and monthly forecast.
- [x] Run forecast tests and verify the local server responds.
- [x] Provide local run instructions and deliver the application for review.
