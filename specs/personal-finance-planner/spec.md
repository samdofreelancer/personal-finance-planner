# Feature Specification: Personal Finance Planner (2026–2030)

**Status:** Redesigning — Three-tier architecture  
**Stack:** Spring Boot (Java 21) backend + PostgreSQL database + Vue.js frontend  
**Communication:** REST API (frontend → backend)

## 1. Purpose

Private financial forecast for Nov 2026 – Dec 2030. User edits income, raises, expenses, loan details, starting savings; sees monthly & annual cash-flow outcomes and savings progress.

## 2. Users and scope

- Single household user; no sign-in.
- Local deployment: Spring Boot backend + PostgreSQL + Vue.js frontend.
- Assumptions & forecast persisted in PostgreSQL (single user row).
- No bank sync, cloud, payments, or financial advice.

## 3. User stories

1. Edit financial assumptions via Vue; forecast recalculates via Spring Boot REST API.
2. Change each April raise independently (0%–100%).
3. Change January salary multiplier independently.
4. Enter bank-reported interest for Nov/Dec 2026 and Jan 2027 payoff.
5. View monthly net cash flow and cumulative savings/shortfall.
6. Compare annual net cash flow 2026–2030 in chart and table.
7. Distinguish partial year (2026) and unconfirmed payoff interest.
8. Reset to defaults; export CSV.

## 4. Initial assumptions (VND)

| Assumption | Value |
|---|---:|
| Forecast period | Nov 2026–Dec 2030 |
| 2026 base monthly salary | [REDACTED] |
| April raises 2027–2030 | 10% each, editable |
| January salary multiplier | 2.5×, editable |
| Living expenses | [REDACTED]/month |
| Child fund through Dec 2026 | [REDACTED]/month |
| Child fund Jan–Mar 2027 | [REDACTED]/month |
| Child fund from Apr 2027 | [REDACTED]/month |
| Rent through Jan 2027 | [REDACTED]/month |
| Rent from Feb 2027 | [REDACTED]/month |
| Starting savings | [REDACTED] |
| Loan principal ([REDACTED]) | [REDACTED] |
| Fixed Nov/Dec 2026 payment | [REDACTED]/month |
| Bank interest Nov 2026 | [REDACTED] |
| Bank interest Dec 2026 | [REDACTED] |
| Payoff date | [REDACTED] |
| Payoff interest (workbook) | [REDACTED] (unconfirmed) |

## 5. API Contract

| Method | Endpoint | Request | Response |
|--------|----------|---------|----------|
| GET | `/api/assumptions` | — | `AssumptionDTO` |
| PUT | `/api/assumptions` | `AssumptionDTO` | `AssumptionDTO` |
| GET | `/api/forecast` | query params | `ForecastResponse { months[], annual[] }` |
| GET | `/api/export/csv` | — | `text/csv` download |
| POST | `/api/seed` | — | seed defaults if empty |

## 6. Calculation rules (ported from forecast.mjs)

- `Total loan payment = principal + interest`
- `Total expenses = living + child + rent + loan payment`
- `Net cash flow = salary − total expenses`
- Cumulative savings = starting savings + cumulative net cash flow
- Nov/Dec 2026: principal = fixed payment − bank interest
- Jan 2027 payoff: full remaining principal + payoff interest
- Missing/unconfirmed payoff interest → January marked provisional
- Integer VND rounding; negative savings shown as shortfall

## 7. Acceptance criteria

1. 50 monthly rows + 5 annual rows (2026–2030).
2. Frontend edits persist to PostgreSQL and recalculate via API.
3. Nov/Dec principal from fixed payment − bank interest; Jan payoff = full principal.
4. Cumulative savings includes negative January payoff month.
5. Unconfirmed payoff interest visibly flagged provisional.
6. Annual chart labels 2026–2030; 2026 marked partial.
7. Reload preserves inputs (from DB); reset restores defaults; CSV matches display.

## 8. Out of scope

- Bank sync, multi-user, cloud, investment/tax/inflation modeling, online deployment.