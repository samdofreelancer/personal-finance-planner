# 04. API Contract

Base path: `/api`. In Docker/local production-like use, the browser calls Nginx and Nginx proxies `/api` to Spring Boot. Backend port 8080 is also published by the current compose file.

## Endpoints (as-is)

| Method | Path | Input | Persistence side effect | Response |
|---|---|---|---|---|
| GET | `/api/assumptions` | None | May create an in-memory default entity if no row exists; current code path does not explicitly save that entity | `AssumptionDTO` |
| PUT | `/api/assumptions` | JSON `AssumptionDTO` | Saves/replaces the single assumptions row | `AssumptionDTO` |
| POST | `/api/assumptions/seed` | None | Writes seed defaults over the current row | `AssumptionDTO` |
| GET | `/api/forecast` | Query-bound `AssumptionDTO` fields | None | `ForecastResponse` (`months`, `annual`) |
| GET | `/api/export/csv` | Query-bound `AssumptionDTO` fields | None | `text/csv` attachment |

## Forecast input semantics — important

Current implementation of `ForecastController.get()` and `exportCsv()` binds the request query parameters to an `AssumptionDTO`, validates it and calculates directly from that DTO. It does **not** load the saved assumptions from PostgreSQL for these operations.

The frontend currently sends its current form DTO as query parameters for preview, refresh and CSV export. This is how temporary preview works today, but the API does not yet express a separate persisted-forecast versus simulation contract. This is tracked by finding F-09.

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
| `loanPrincipal` | integer | Opening loan principal in VND |
| `fixedPayment` | integer | Fixed payment for Nov/Dec 2026 |
| `novInterest`, `decInterest` | integer | Bank interest for Nov/Dec 2026 |
| `payoffInterest` | integer or null | Estimated/confirmed payoff interest |
| `payoffInterestConfirmed` | boolean | Whether payoff interest has been verified |
| `startingSavings` | integer | Starting savings in VND |
| `raises` | object | Annual raise rates keyed by 2027, 2028, 2029, 2030 |

## Forecast response

- `months`: 50 monthly rows. Each row includes salary, expense components, loan principal/interest/payment, total expenses, net cash flow, cumulative savings and provisional flags.
- `annual`: 5 annual rows. Each row includes year, partial-year flag, salary total, expense total, net cash flow, ending savings and provisional flags.
- Exact serialized field names are defined by `ForecastMonthDTO`, `ForecastAnnualDTO` and `ForecastResponse`; update this document when those DTOs change.

## Error behavior

- Invalid assumptions are rejected by `ValidationService`.
- `ApiExceptionHandler` maps API exceptions to HTTP error responses. Keep error shape stable and actionable for the frontend.
- Frontend `api.js` attempts to read a JSON `message`; otherwise it uses a fallback message.

## Contract change policy

- Do not silently change query parameter names, DTO fields, nullability or forecast semantics.
- Change backend and frontend consumers in one traceable feature.
- Add contract/integration tests for externally observable behavior.
- The desired future contract must explicitly separate forecast from saved assumptions and non-persisting simulation; see F-09 in the remediation spec.
