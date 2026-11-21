# Feature Specification: Effective-Month Income Schedule

**Status:** Accepted for implementation

## User value

Users can enter explicit monthly income amounts and the month each amount takes effect. Forecasts then use the latest effective income amount instead of compounding assumed annual percentage raises.

## Decisions

- The forecast baseline starts in `2026-11`; every income schedule must include exactly one baseline entry for that month.
- Each entry contains an effective month (`YYYY-MM`) and a non-negative whole-VND monthly amount.
- Multiple future changes are supported. The latest effective entry at or before a forecast month determines that month's base income.
- Entries beyond the selected forecast horizon remain saved when the horizon is shortened.
- Synthetic first-run/reset defaults retain the former demonstration forecast as explicit fixed amounts through April 2032; these are editable schedule entries, not percentage calculations at forecast time.
- The user can add, edit and delete entries other than the required `2026-11` baseline.
- `januaryMultiplier` remains unchanged and is applied to the effective monthly amount in January.
- Forecast preview and CSV export use the supplied DTO snapshot and do not persist it. Only explicit Save persists the full schedule.
- Existing base salary and annual raise assumptions are migrated to dated amounts: a baseline in November 2026 and an April amount for each configured raise year, using compounded prior raise factors and whole-VND HALF_UP rounding.

## User scenarios and acceptance criteria

### Scenario 1 — Configure income changes

Given the user opens income assumptions, when they view the income schedule, then they can see a baseline entry for 11/2026 and add future entries with an effective month and monthly amount.

Given the user adds entries for 04/2027 at 60,000,000 VND and 01/2028 at 65,000,000 VND after a 11/2026 baseline of 50,000,000 VND, then all three entries are shown and can be edited or removed except the required baseline.

### Scenario 2 — Apply effective amounts

Given entries `2026-11: 50,000,000`, `2027-04: 60,000,000`, and `2028-01: 65,000,000`, then the base forecast income is 50M from Nov 2026 through Mar 2027, 60M from Apr through Dec 2027, and 65M from Jan 2028 onward.

Given January multiplier is 2.0, then January salary is twice the effective monthly amount for that January; other months use the effective amount unchanged.

### Scenario 3 — Persist and preview

When the user edits income entries and previews the forecast, the forecast reflects the unsaved schedule without persisting it. When the user saves and reloads assumptions, the full schedule is restored.

Given the user shortens the forecast horizon and later extends it, future income entries outside the shortened horizon remain saved and become visible again.

### Scenario 4 — Migrate existing assumptions

Given an existing database has a base salary and annual raise rates, when V6 runs, then it creates a November 2026 baseline and equivalent April income amounts for configured raise years, preserves January multiplier and all unrelated assumptions, and removes the retired percentage-based fields.

## Functional requirements

- FR-001: Replace `baseSalary` and `raises` in the assumptions contract with `incomeChanges[]` containing `effectiveMonth` and `amount`.
- FR-002: Require exactly one `2026-11` baseline entry.
- FR-003: Support any number of unique future effective-month entries from November 2026 onward, including entries beyond the current forecast horizon.
- FR-004: For each forecast month, use the amount from the latest effective entry less than or equal to that month.
- FR-005: Apply `januaryMultiplier` to the effective amount in January only.
- FR-006: Allow adding, editing and deleting future entries in the assumptions UI; the baseline cannot be deleted.
- FR-007: Persist the complete schedule with `PUT /api/assumptions`; keep preview and CSV export non-persisting.
- FR-008: Validate schedule requirements on the backend and reject invalid saves atomically.
- FR-008a: Reject fractional JSON VND amounts instead of allowing Jackson to truncate them to integers.
- FR-009: Migrate existing base salary and annual raise assumptions to equivalent effective-month amounts without losing unrelated data.
- FR-010: Keep forecast horizon, expense schedules, loan schedules, dashboard output and CSV behavior aligned with the new income schedule.

## Validation and edge cases

- Reject a missing or empty schedule and a missing `2026-11` baseline.
- Reject invalid calendar months, months before November 2026, duplicate effective months, null/negative amounts and fractional VND amounts.
- A future entry applies starting in its effective month, not the preceding month.
- An entry after the selected forecast horizon remains persisted and must not affect earlier months.
- Deleting a future entry restores the previous entry's amount from the next month onward.
- Invalid PUT leaves the previous assumptions and schedule unchanged.
- Migration must handle all existing assumptions rows and preserve whole-VND rounding deterministically.

## Out of scope

- Percentage or compound annual raise inputs.
- Automatic inflation or extrapolation beyond configured entries.
- Multiple income sources, tax calculations or probabilistic salary scenarios.

## Measurable success criteria

- Backend tests prove month-boundary lookup, January multiplier, schedule validation and migration conversion.
- API integration tests prove save/reload, invalid-save atomicity and non-persisting forecast preview.
- Frontend tests prove add/edit/remove and payload serialization.
- Playwright proves an income schedule can be edited, saved, reloaded and reflected in the forecast.
- Existing forecast horizon and expense/loan tests remain green.
