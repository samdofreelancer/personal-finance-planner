# Implementation Plan: Personal Finance Planner

**Status:** Implemented and verified.

## Proposed approach

- Build a lightweight, dependency-free JavaScript web application with a Python standard-library local server. This keeps the deliverable self-contained in the workspace-less chat while retaining a testable calculation module.
- Keep calculation logic in a pure ES module separate from the UI so it can be tested independently with Node.js.
- Use a responsive single-page interface with:
  - editable assumptions panel;
  - annual net-cash-flow chart;
  - annual summary;
  - monthly forecast table.
- Render the annual chart as local SVG, with correct year labels, VND values, a zero baseline, and accessible text descriptions; no chart package or network dependency.
- Persist approved user assumptions in browser local storage. Include reset and CSV download; do not add a backend.
- Keep all financial computation client-side and provide a clear indication that projections depend on user-provided assumptions.

## Calculation and validation design

- Represent money internally as integer VND; reject fractional, negative, or unsafe money values.
- Represent raise rates and January multiplier as validated numeric inputs.
- Generate the monthly date sequence from 2026-11 through 2030-12.
- Apply each April raise to subsequent salary months (including that year's January salary only insofar as previous years' raises already apply).
- Calculate observed-interest loan payments as specified in the feature spec; do not infer missing bank amounts from the nominal rate.
- Use `null` for unconfirmed payoff interest, not zero. Mark January 2027 net cash flow and all subsequent cumulative savings provisional until confirmed. The UI may show a provisional calculation excluding the unknown interest, but must show that it is incomplete.
- Use integer arithmetic for inputs and payment amounts; define how fractional salary results from raises are rounded (proposed: round to nearest VND for displayed salary and all downstream calculations).

## Suggested structure

- `personal-finance-planner/forecast.mjs` — pure forecast calculation and input validation.
- `personal-finance-planner/test/forecast.test.mjs` — acceptance and edge-case tests.
- `personal-finance-planner/app.mjs` — editable inputs, summaries, chart, persistence, and CSV export.
- `personal-finance-planner/index.html` / `styles.css` — local responsive interface.
- `personal-finance-planner/serve.py` — loopback-only static server.

## Verification

- Unit tests for salary raise timing, January multiplier, expense phase changes, loan principal and payoff, negative savings, partial-year totals, and missing payoff interest.
- Verify the forecast has 50 months and annual rows for 2026–2030.
- Verify annual chart inputs exactly match annual table values and labels.
- Verify persistence, reset, and CSV export in a local browser.
- Run the repository's existing lint, type-check, tests, and build after implementation.

## Decisions applied

1. Use 2.5× as the startup January multiplier because it is the latest value saved in the workbook; keep editable (the original requested default was 2×).
2. Load 3,683,193 VND as the workbook's current payoff-interest value, without claiming it is bank-confirmed; allow clearing it to mark projections provisional.
3. Use local-only storage and CSV export; no online deployment or external network dependency.
4. Round raised salary to the nearest VND each month; all other monetary inputs and results use integer VND.
