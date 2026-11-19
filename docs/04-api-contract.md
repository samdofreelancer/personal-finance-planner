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
| GET | `/api/forecast` | Query-bound `AssumptionDTO` fields | None | `ForecastResponse` (`months`, `annual`); loan schedules loaded from PostgreSQL |
| GET | `/api/export/csv` | Query-bound `AssumptionDTO` fields | None | `text/csv` attachment; uses the same saved loan schedules as forecast |

## Forecast input semantics — important

Current implementation of `ForecastController.get()` and `exportCsv()` binds the request query parameters to an `AssumptionDTO`, validates it and calculates salary/expense assumptions directly from that DTO. Both operations load the persisted loan schedules through `LoanScheduleService`; they do not load the saved assumptions row for preview calculations.

The frontend sends its current income/expense assumptions as query parameters for preview, refresh and CSV export. Loan schedules are managed and saved separately through `PUT /api/loans`; unsaved loan edits are not included in the forecast.

## Assumptions DTO fields

| Field | Type | Meaning |
|---|---|---|
| `baseSalary` | integer | Base monthly salary in VND |
| `januaryMultiplier` | decimal | January salary multiplier |
| `living` | integer | Monthly living expenses in VND |
| `childThrough2026` | integer | Child expense through 2026 |
| `childQ12027` | integer | Child expense Jan–Mar 2027 |
| `childFromApr2027` | integer | Child expense from Apr 2027 |
| `rentThroughJan2027` | integer | Rent through Jan 2027 |
| `rentFromFeb2027` | integer | Rent from Feb 2027 |
| `startingSavings` | integer | Starting savings in VND |
| `raises` | object | Annual raise rates keyed by 2027, 2028, 2029, 2030 |

## Loan schedule DTOs

- `LoanDTO`: `id` (null for a new loan), `name`, `openingPrincipal`, `repayments[]`.
- `LoanRepaymentDTO`: `id` (null for a new entry), `dueDate` (`YYYY-MM-DD`), `principalAmount`, `interestAmount`.
- `PUT /api/loans` replaces the complete collection in one transaction. Keep IDs for records being edited; omit an existing loan/entry to delete it. IDs must belong to the current single-household collection and parent loan.
- The frontend validates for usability; backend validation is authoritative. Invalid payloads leave the previous collection unchanged.

## Forecast response

- `months`: 50 monthly rows. Each row includes salary, expense components, aggregated loan principal/interest/payment, total expenses, net cash flow, cumulative savings and provisional flags.
- `annual`: 5 annual rows. Each row includes year, partial-year flag, salary total, expense total, net cash flow, ending savings and provisional flags.
- Exact serialized field names are defined by `ForecastMonthDTO`, `ForecastAnnualDTO` and `ForecastResponse`; update this document when those DTOs change.

## Error behavior

- Invalid assumptions are rejected by `ValidationService`; invalid loan schedules are rejected by `LoanScheduleService`.
- `ApiExceptionHandler` maps API exceptions to HTTP error responses. Keep error shape stable and actionable for the frontend.
- Frontend `api.js` attempts to read a JSON `message`; otherwise it uses a fallback message.

## Contract change policy

- Do not silently change query parameter names, DTO fields, nullability or forecast semantics.
- Change backend and frontend consumers in one traceable feature.
- Add contract/integration tests for externally observable behavior.
- The desired future contract must explicitly separate forecast from saved assumptions and non-persisting simulation; see F-09 in the remediation spec.
