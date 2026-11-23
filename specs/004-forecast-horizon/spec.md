# Feature Specification: Configurable Forecast Horizon

**Status:** Implemented and verified; income behavior is superseded by `specs/005-income-schedule/spec.md`.

## User value

The household can choose how far ahead the financial outlook should run, without being limited to December 2030. Income growth assumptions remain explicit for every forecast year.

## Decisions

- The forecast starts in November 2026, preserving the existing product baseline.
- The default horizon is 4 years and preserves the existing November 2026–December 2030 output.
- A horizon of N years includes the partial starting year 2026 plus N complete calendar years, ending in December 2026+N. Therefore the forecast contains `12*N + 2` months and N+1 annual summaries.
- The user can select 1, 2, 3, 4, 5 or 6 years.
- The selected horizon and the complete effective-month income schedule are saved with household assumptions in the database. Forecast preview remains non-persisting until the user saves assumptions.
- Income entries beyond the selected horizon remain saved when the horizon is shortened, so extending it later restores those entries.
- Changing the horizon updates monthly and annual forecast, dashboard KPIs, charts, tables and CSV export consistently.

## User scenarios and acceptance criteria

### Scenario 1 — Select a forecast horizon

Given the default saved assumptions, when the user selects 1 year and previews or saves, then the outlook runs from November 2026 through December 2027 (14 months, 2 annual rows).

Given the user selects 4 years, then the existing November 2026–December 2030 period remains unchanged (50 months, 5 annual rows).

Given the user selects 6 years, then the outlook runs through December 2032 (74 months, 7 annual rows).

### Scenario 2 — Preserve income schedule across horizon changes

Given the user has configured an income change after the selected end year, when the user shortens the horizon and later extends it again, then the future income entry remains saved and is available to the forecast.

### Scenario 3 — Persist and preview

When the user changes the horizon, then the preview reflects it without persisting it. When the user saves, reload restores the selected horizon and the complete income schedule.

### Scenario 4 — Consistent outputs

For every supported horizon, monthly rows, annual rows, final-period savings, year-specific KPI labels, chart/table data and CSV rows cover the same start and end dates. The partial 2026 year remains explicitly marked.

## Functional requirements

- FR-001: Provide exactly six horizon choices: 1–6 years.
- FR-002: Default the horizon to 4 years for existing and new installations.
- FR-003: Persist the selected horizon with household assumptions.
- FR-004: Generate monthly periods from November 2026 through December of the selected end year.
- FR-005: Generate annual summaries for 2026 through the selected end year, marking 2026 partial.
- FR-006: Preserve all configured income schedule entries outside the active horizon when saving a shorter horizon.
- FR-007: Apply the selected effective-month income amount to each forecast month; income schedule details are defined in `specs/005-income-schedule/spec.md`.
- FR-008: Apply the January multiplier after selecting the effective monthly income amount.
- FR-009: Keep forecast preview and CSV simulation non-persisting; only explicit assumptions save persists the horizon and income schedule.
- FR-011: Keep all dashboard and export outputs aligned to the selected end year.

## Validation and edge cases

- Reject horizons outside the integer range 1–6.
- A shortened horizon must not delete saved income schedule entries for later months.

## Out of scope

- A user-configurable forecast start date.
- Automatic income growth or extrapolation beyond configured schedule entries.
- Inflation, investment returns, tax or salary uncertainty modeling.

## Measurable success criteria

- All six horizon choices return the exact expected monthly and annual row counts.
- Existing installations retain equivalent income levels through the V6 migration; the 4-year horizon still returns 50 monthly rows and five annual summaries.
- Horizon and income schedule survive API save and reload.
- No KPI, chart, table or CSV section displays a fixed 2030 endpoint when another horizon is selected.
