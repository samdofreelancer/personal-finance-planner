# 04. API Contract

Base path: `/api`. In Docker/local production-like use, the browser calls Nginx and Nginx proxies `/api` to Spring Boot. Backend port 8080 is also published by the current compose file.

## Endpoints (as-is)

| Method | Path | Input | Persistence side effect | Response |
|---|---|---|---|---|
| GET | `/api/assumptions` | None | Creates persisted defaults only when no assumptions row exists (fresh installs are initialized by Flyway) | `AssumptionDTO` |
| PUT | `/api/assumptions` | JSON `AssumptionDTO` | Saves/replaces the single assumptions row | `AssumptionDTO` |
| POST | `/api/assumptions/seed` | None | Resets assumptions and replaces loan schedules with the synthetic sample schedule | `AssumptionDTO` |
| GET | `/api/loans` | None | None | `LoanDTO[]` with nested repayment entries |
| PUT | `/api/loans` | Complete `LoanDTO[]` collection | Atomically updates supplied IDs, inserts new records and deletes omitted records | Saved `LoanDTO[]` |
| POST | `/api/forecast` | JSON `AssumptionDTO` including `expenses[]` | None | `ForecastResponse` (`months`, `annual`); loan schedules loaded from PostgreSQL |
| POST | `/api/export/csv` | JSON `AssumptionDTO` including `expenses[]` | None | `text/csv` attachment using the supplied expense schedules |

## Forecast input semantics — important

The frontend sends a complete assumptions snapshot as JSON to `POST /api/forecast` and `POST /api/export/csv`. The backend validates that snapshot and calculates salary and expense assumptions directly from it. Both operations load persisted loan schedules through `LoanScheduleService`; they do not load saved assumptions for preview calculations.

`PUT /api/assumptions` persists the complete income schedule, January multiplier, horizon, savings and expense-category collection in one transaction. Loan schedules are managed separately through `PUT /api/loans`; unsaved loan edits are not included in the forecast.

## Assumptions DTO fields

| Field | Type | Meaning |
|---|---|---|
| `incomeChanges` | array | Required schedule entries with `effectiveMonth` (`YYYY-MM`) and `amount` (non-negative whole VND); must include `2026-11` |
| `januaryMultiplier` | decimal | January salary multiplier |
| `startingSavings` | integer | Starting savings in VND |
| `forecastYears` | integer | Number of complete calendar years after partial 2026; 1–6, default 4 |
| `expenses` | array | User-defined categories with `name` and `changes[]`; each change has `effectiveMonth` (`YYYY-MM`) and `amount` (whole VND) |

Income changes may be configured beyond the active forecast horizon and are retained when the horizon is shortened. For each forecast month, the latest effective amount is used. `januaryMultiplier` is applied after selecting that month's effective amount.

## Loan schedule DTOs

- `LoanDTO`: `id` (null for a new loan), `name`, `openingPrincipal`, `repayments[]`.
- `LoanRepaymentDTO`: `id` (null for a new entry), `dueDate` (`YYYY-MM-DD`), `principalAmount`, `interestAmount`.
- `PUT /api/loans` replaces the complete collection in one transaction. Keep IDs for records being edited; omit an existing loan/entry to delete it. IDs must belong to the current single-household collection and parent loan.
- The frontend validates for usability; backend validation is authoritative. Invalid payloads leave the previous collection unchanged.

## Forecast response

- `months`: `12 * forecastYears + 2` monthly rows (50 by default), from Nov 2026 through Dec of the selected end year. Each row includes salary, `recurringExpenses`, `expenseBreakdown[]` (`name`, `amount`), aggregated loan principal/interest/payment, total expenses, net cash flow, cumulative savings and provisional flags.
- `annual`: `forecastYears + 1` annual rows (5 by default), from 2026 through the selected end year. Each row includes year, partial-year flag, salary total, expense total, net cash flow, ending savings and provisional flags.
- Exact serialized field names are defined by `ForecastMonthDTO`, `ForecastAnnualDTO` and `ForecastResponse`; update this document when those DTOs change.

## Error behavior

- Invalid assumptions are rejected by `ValidationService`; invalid loan schedules are rejected by `LoanScheduleService`.
- `ApiExceptionHandler` maps API exceptions to HTTP error responses. Keep error shape stable and actionable for the frontend.
- Frontend `api.js` attempts to read a JSON `message`; otherwise it uses a fallback message.

## Contract change policy

- Do not silently change query parameter names, DTO fields, nullability or forecast semantics.
- Change backend and frontend consumers in one traceable feature.
- Add contract/integration tests for externally observable behavior.
- The POST contract explicitly separates non-persisting forecast simulation from `PUT /api/assumptions`; keep the full forecast input as a JSON snapshot so new expense schedules are represented without query-parameter encoding.
