# Flexible Loan Repayment and Separate Savings Design

## Intent

Separate loan management from savings in the assumptions UI. Let the household record one or more loans, each with its own manually maintained repayment schedule, so irregular payment dates and amounts can be represented and included in the monthly forecast.

## Goals

- Present **Khoản vay** and **Tiết kiệm** as separate sections in the financial assumptions form.
- Support multiple independent loans.
- Let users add, edit and remove any number of repayment periods for each loan.
- Record each scheduled payment date, principal amount and interest amount separately.
- Persist loans and their schedule entries and use scheduled amounts in the forecast month matching each payment date.
- Keep starting savings as a separate savings assumption.

## Non-goals

- Bank integration, automatic schedule generation, amortization calculators or financial advice.
- Recording bank transactions or reconciling actual account balances.
- Changing income, household expenses or salary raise behavior.

## Approaches

1. **User-managed schedule per loan (selected):** each loan owns editable dated payment entries with principal and interest. This supports irregular schedules and multiple loans with clear ownership.
2. Recurring schedule template: generate payments from a frequency and fixed amount. This is simpler for regular loans but does not fit ad-hoc payments without exceptions and overrides.

The selected approach follows the user's choice of A.

## Proposed behavior

- A loan has a name, opening principal and zero or more repayment entries.
- A repayment entry has a due date, principal amount and interest amount. Amounts are non-negative whole VND; the displayed total is principal plus interest.
- Users can add, edit and delete entries independently within each loan.
- Multiple entries may fall in the same month; the forecast aggregates them into that month's principal, interest and total loan payment.
- Scheduled entries inside the forecast horizon contribute to the corresponding forecast month. Entries outside the horizon remain saved but do not affect the displayed forecast.
- The existing forecast's hard-coded Nov/Dec 2026 installments and Jan 2027 payoff behavior are replaced by schedule-driven loan payments. The forecast must retain the existing monthly and annual response shape where possible.
- Savings remains a separate section containing the starting savings amount; cumulative savings continues to be derived from monthly net cash flow.
- The existing single-household/local-first product boundary remains in place.

## Implementation calls

- Model loans and repayment entries as separate persisted records with a loan-to-entry relationship, rather than adding a fixed number of payment fields to the assumptions row.
- Expose loan CRUD and schedule operations through explicit REST contracts; forecast reads the persisted loan schedules for the saved scenario.
- Update seed/default behavior, validation, frontend draft/save flow, forecast calculations, CSV export, docs and tests together.
- Provide a safe schema evolution path and preserve existing assumptions during migration. Existing sample loan fields must be migrated into one sample loan with equivalent dated schedule entries so current forecast behavior is not silently lost.
- The builder will determine exact API routes, cascade/delete behavior and compatibility strategy after inspecting repository conventions; any change that would discard user-entered data must be avoided.

## Acceptance criteria

1. The form shows distinct Khoản vay and Tiết kiệm sections.
2. A user can maintain multiple loans and arbitrary dated repayment entries per loan.
3. Invalid dates or negative/non-integer money values are rejected by backend validation.
4. Multiple entries in one month aggregate correctly into principal, interest and total payment.
5. Forecast, annual summary and CSV reflect the schedule-driven payments consistently.
6. Existing saved assumptions survive schema evolution and the migrated sample schedule preserves its current monthly payment timing and amounts.
7. Automated tests cover CRUD/validation, migration or mapping behavior, forecast month boundaries, multiple payments in one month, and savings cash-flow effects.
8. Documentation describes the updated business rules, API and data model.

## Open implementation choices

- **Paid/planned status — resolved:** no status field. Entries are planned forecast inputs; a paid/unpaid ledger is out of scope.
- **API save contract — resolved:** `GET /api/loans` reads the collection and `PUT /api/loans` atomically replaces it. Existing IDs are retained for updates; omitted IDs are deleted; null IDs create records. Backend validates the entire payload before committing.
- **Forecast input source — resolved:** non-loan assumptions remain query-bound for preview compatibility; loan schedules are read from PostgreSQL and are included in forecast/CSV only after the user saves them.
- **Schema lifecycle — resolved:** introduce Flyway V1 baseline and V2 backfill of the former single-loan fields, then V3 drops those retired columns. Existing assumptions and non-loan values remain intact.
