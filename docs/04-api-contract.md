# 04. API Contract

Base path: `/api`. In Docker/local production-like use, the browser calls Nginx and Nginx proxies `/api` to Spring Boot. Backend port 8080 is also published by the current compose file.

## Endpoints (as-is)

| Method | Path | Input | Persistence side effect | Response |
|---|---|---|---|---|
| GET | `/api/auth/bootstrap` | None | None | `{"registrationAvailable":true}`; retained for older clients, registration is always open |
| POST | `/api/auth/register` | `{email,password}` or `{email,password,invitationToken}` | Creates user + new household + OWNER membership, or consumes invitation + creates MEMBER membership | `AuthResponse` and authenticated session (`201`) |
| POST | `/api/auth/login` | `{email,password}` | Creates authenticated session | `AuthResponse` (`200`) |
| POST | `/api/auth/logout` | CSRF token + session cookie | Invalidates session | `204` |
| GET | `/api/auth/me` | Session cookie | None | Current `AuthResponse` |
| GET | `/api/profile` | Authenticated session | None | `ProfileResponse` for the authenticated user; excludes password hash |
| PUT | `/api/profile` | `{displayName,avatarUrl}` + CSRF token | Updates the authenticated user's display name and optional HTTPS avatar URL | Updated `ProfileResponse` |
| POST | `/api/profile/password` | `{currentPassword,newPassword}` + CSRF token | Replaces the authenticated user's password hash after verifying the current password | `204` |
| GET | `/api/assumptions` | Authenticated household session | Creates a blank financial profile only when the household has no assumptions row yet | `AssumptionDTO` |
| PUT | `/api/assumptions` | JSON `AssumptionDTO` | Saves/replaces the single assumptions row | `AssumptionDTO` |
| POST | `/api/assumptions/seed` | None | Resets assumptions and replaces loan schedules with the synthetic sample schedule | `AssumptionDTO` |
| GET | `/api/loans` | None | None | `LoanDTO[]` with nested repayment entries |
| PUT | `/api/loans` | Complete `LoanDTO[]` collection | Atomically updates supplied IDs, inserts new records and deletes omitted records | Saved `LoanDTO[]` |
| POST | `/api/forecast` | JSON `AssumptionDTO` including `expenses[]` | None | `ForecastResponse` (`months`, `annual`); loan schedules loaded from PostgreSQL |
| POST | `/api/export/csv` | JSON `AssumptionDTO` including `expenses[]` | None | `text/csv` attachment using the supplied expense schedules |

Authenticated sessions are stored in Redis (`planwise:session`) through Spring Session Redis. The inactivity timeout is 30 minutes and is refreshed by authenticated requests; Redis AOF and its persistent Compose volume keep sessions available across backend/Redis restarts until logout or expiry.

## Forecast input semantics — important

The frontend sends a complete assumptions snapshot as JSON to `POST /api/forecast` and `POST /api/export/csv`. The backend validates that snapshot and calculates salary and expense assumptions directly from it. Both operations load persisted loan schedules through `LoanScheduleService`; they do not load saved assumptions for preview calculations.

`PUT /api/assumptions` persists the complete income schedule, January multiplier, horizon, savings and expense-category collection in one transaction. Loan schedules are managed separately through `PUT /api/loans`; unsaved loan edits are not included in the forecast.

## Registration and household ownership

- Self-registration without `invitationToken` is always enabled. Each successful request creates a new household and assigns the registering user the `OWNER` role; it never attaches that user to the legacy/initial household.
- Registration with a valid `invitationToken` joins the invitation's household as `MEMBER`; the one-time token is consumed.
- The public bootstrap endpoint always reports `registrationAvailable: true`; it does not disclose account counts or user details.
- A new household's first `GET /api/assumptions` creates a blank profile: one `2026-11` income baseline of `0`, `startingSavings: 0`, no expense categories and no loan schedules. Forecast horizon defaults to 4 years and January multiplier to 2.0. The existing reset endpoint remains the explicit action to load synthetic sample data.
- Existing household records are not copied, reset or reassigned by later registrations.

## Personal profile

- Profile endpoints derive `userId` exclusively from the authenticated session principal; clients cannot select or update another user's profile.
- `email` and `createdAt` are read-only account metadata. `displayName` is required (1–80 characters) when updating. `avatarUrl` is optional and accepts HTTPS URLs only.
- Password changes require the current password and enforce the registration policy (at least 12 characters and at most 72 UTF-8 bytes). A new password must differ from the current password.
- Personal profile data is account-level identity data; it is not part of the household or financial assumptions aggregate.

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
