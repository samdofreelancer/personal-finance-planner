# Feature Specification: Forecast UI/UX Refinement

**Status:** Implemented and verified
**Scope:** Presentation and interaction improvements for the existing Planwise financial outlook; financial calculation rules and persistence contracts remain unchanged.

## User value

Users can understand and navigate long forecasts on narrow screens, distinguish editable previews from server-persisted assumptions and loan schedules, and explore a synthetic example without overwriting household data.

## Functional requirements

- FR-001: The monthly forecast keeps the month column visible while horizontally scrolling and keeps cumulative savings visible at the right edge where the table layout permits.
- FR-002: Users can filter monthly rows by every forecast year or all years. Period totals reflect the visible rows; ending savings reflects the final visible month's cumulative savings.
- FR-003: The four loan-detail columns can be shown or hidden. They are initially hidden when the forecast contains no loan activity and remain user-toggleable.
- FR-004: Empty household assumptions expose a prominent sample-data action. Sample assumptions and a sample loan with dated repayments are synthetic and require explicit confirmation.
- FR-005: Loading sample assumptions does not persist assumptions or overwrite saved loans. The sample loan appears as an editable unsaved draft; the monthly forecast continues to use the saved loan schedule until the user explicitly saves the draft.
- FR-006: Monetary inputs display Vietnamese thousands separators while emitting whole-number VND values to the existing state and API contract.
- FR-007: The January multiplier explanation states that it applies only in January to the effective monthly income amount.
- FR-008: Assumptions and loan edits clearly indicate unsaved state. Browser navigation away from a page with unsaved changes triggers the browser's before-unload confirmation.
- FR-009: Annual zero-only cash-flow charts retain a readable zero baseline and show a hint to enter assumptions or load sample data. Footnote markers appear only when the matching partial-year or provisional marker is present.
- FR-010: Vietnamese labels are used consistently across the financial outlook and forecast sections, except the Planwise brand and fixed technical terms.
- FR-011: Existing financial calculations, effective-month income rules, January multiplier, forecast horizon, expense schedules, loan repayment timing, cumulative savings, API shapes and persistence behavior remain unchanged.

## Acceptance criteria

1. Desktop and narrow mobile layouts have no page-level horizontal overflow; monthly tables remain locally scrollable and sticky cells do not obscure text.
2. Selecting a year displays only that year's months and recalculates visible-period totals without changing the underlying forecast response.
3. Toggling loan details changes only column visibility, not any displayed values or forecast data.
4. Loading sample data leaves the persisted assumptions and loan collection unchanged until each explicit save action is used.
5. Editing either assumptions or loan schedules causes the before-unload warning; saved, unchanged state does not.
6. Existing frontend tests, production build and relevant browser E2E checks pass.

## Out of scope

- Changes to financial formulas, rounding, forecast timing, backend domain services, API request/response contracts or database schema.
- Autosaving assumptions or loan schedules.
- Including unsaved loan drafts in the server-generated monthly forecast before the loan schedule is saved.
