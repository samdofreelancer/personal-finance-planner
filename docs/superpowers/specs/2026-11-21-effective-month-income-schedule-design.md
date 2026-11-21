# Effective-Month Income Schedule Design

## Intent

Replace percentage-based annual salary raises with explicit monthly income amounts and effective months. The household needs to model known income changes directly rather than assume an annual growth rate.

## Goals

- Let the user define multiple monthly income amounts with `YYYY-MM` effective months.
- Use the latest effective amount at or before each forecast month.
- Support adding, editing and deleting future changes, with explicit Save semantics.
- Persist the schedule with the existing household assumptions and use it consistently for preview, forecast and CSV export.
- Preserve the selected 1–6 year forecast horizon and January multiplier.
- Migrate existing base salary and annual raise assumptions into equivalent dated income amounts.

## Non-goals

- Salary percentage, inflation or automatic growth projections.
- Probabilistic income scenarios or multiple income sources.
- Changes to expense, loan, savings or forecast-horizon behavior.

## Approaches

1. **Normalized `income_amount_changes` table (recommended):** one row per effective month, following the existing expense schedule pattern. It provides database constraints, straightforward CRUD through the existing assumptions snapshot and deterministic lookup by month.
2. JSON schedule stored on `assumptions`: fewer tables, but weaker relational constraints and inconsistent with the normalized expense schedule already in this project.
3. Keep `baseSalary` plus a month-keyed map: smaller migration but retains two competing representations of income and complicates validation and UI semantics.

Use approach 1. The existing application already models user-defined expense changes as normalized effective-month rows; reusing that pattern minimizes new concepts and keeps forecast input as a complete DTO snapshot.

## Domain and API design

- Replace `baseSalary` and `raises` in `AssumptionDTO` with `incomeChanges: [{ effectiveMonth: "YYYY-MM", amount: integerVnd }]`.
- `effectiveMonth` is a calendar month, not a day. The initial required baseline is `2026-11` because the forecast starts in November 2026.
- Amounts are non-negative whole VND. Duplicate effective months are invalid. A schedule must contain exactly one `2026-11` baseline; later entries can be any valid month from `2026-11` onward, including months beyond the currently selected horizon, and are retained when the horizon changes.
- The forecast uses the latest schedule entry whose effective month is less than or equal to the forecast month. The baseline requirement guarantees a value for every forecast month.
- `januaryMultiplier` remains independent and is applied to the effective monthly income in January only.
- `GET/PUT /api/assumptions`, `POST /api/forecast` and `POST /api/export/csv` use the new `incomeChanges` field. Preview and export remain non-persisting; only PUT saves.
- The UI provides add/remove/edit controls for schedule entries and keeps the current save/reset interaction model.
- The schedule is attached to the current single-household `Assumption` record. This repository does not yet have authenticated user/tenant ownership; do not invent a user identity or imply multi-user isolation.
- Synthetic first-run/reset defaults retain the former demonstration forecast as fixed amounts (42M baseline; 45.36M from 04/2027; compounded fixed values through 04/2032). This preserves the sample forecast while eliminating runtime percentage growth; users can edit or remove future entries.

## Migration

Add Flyway V6 after the uncommitted V5 horizon migration. For every existing assumptions row, create a `2026-11` entry equal to `base_salary`; for each year 2027–2032 create an April entry equal to the base salary multiplied by all annual raise factors through that year, rounded to whole VND using HALF_UP. Then remove the old `salary_raise_rates` table and `base_salary` column. Keep `january_multiplier`, `forecast_years`, savings, expenses and loans unchanged. Migration tests must cover the conversion and ensure no assumption row is lost.

The migrated April amounts preserve the configured monthly income levels; the January multiplier continues to apply at forecast time. Because the new model stores whole-VND effective amounts, January rounding is based on the rounded effective monthly amount.

## Validation and error handling

- Reject null or empty schedules, missing `2026-11`, invalid calendar months, months before `2026-11`, duplicate months, null/negative amounts and non-whole-VND values where DTO serialization permits decimals.
- Configure Jackson to reject floating-point JSON tokens for integer VND DTO fields instead of silently truncating them.
- Validate on the backend for both persisted assumptions and forecast snapshots; frontend validation is for usability only.
- Invalid PUT requests must leave the previously saved assumptions and income schedule unchanged.

## Acceptance examples

Given entries `2026-11: 50,000,000`, `2027-04: 60,000,000`, `2028-01: 65,000,000`, forecast salaries before January adjustment are 50M in Nov 2026–Mar 2027, 60M from Apr–Dec 2027, and 65M from Jan 2028 onward. January salary is multiplied by `januaryMultiplier` after selecting the effective amount.

## Implementation calls

- Use the existing Spring Boot/JPA/Flyway and Vue 3 patterns; no new dependencies.
- Use a dedicated `IncomeScheduleService` analogous to `ExpenseScheduleService`.
- Keep forecast arithmetic in `ForecastService`; do not duplicate it in JavaScript.
- Preserve all pre-existing working-tree changes. Do not commit or push unless requested.
