# Feature Specification: Personal Finance Planner (2026–2030)

**Status:** Redesigning — Three-tier architecture  
**Stack:** Spring Boot (Java 21) backend + PostgreSQL database + Vue.js frontend  
**Communication:** REST API (frontend → backend)

## 1. Purpose

Personal financial forecast for a configurable planning period. User edits income, raises, expenses, loan details, and starting savings; sees monthly and annual cash-flow outcomes and savings progress. Repository defaults are synthetic demonstration values.

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

## 4. Sample assumptions (VND)

| Assumption | Value |
|---|---:|
| Forecast period | Nov 2026–Dec 2030 |
| Base monthly salary | 42,000,000 |
| Annual raises | 8% each, editable |
| January salary multiplier | 2.0×, editable |
| Living expenses | 4,250,000/month |
| Child fund, first period | 2,500,000/month |
| Child fund, second period | 2,750,000/month |
| Child fund, later period | 3,250,000/month |
| Rent, first period | 3,750,000/month |
| Rent, later period | 5,750,000/month |
| Starting savings | 30,000,000 |
| Sample loan principal | 120,000,000 |
| Fixed payment | 10,000,000/month |
| First period interest | 1,200,000 |
| Second period interest | 1,100,000 |
| Estimated payoff interest | 1,000,000 (unconfirmed) |

## 5. API Contract

| Method | Endpoint | Request | Response |
|--------|----------|---------|----------|
| GET | `/api/assumptions` | — | `AssumptionDTO` |
| PUT | `/api/assumptions` | `AssumptionDTO` | `AssumptionDTO` |
| GET | `/api/forecast` | query params | `ForecastResponse { months[], annual[] }` |
| GET | `/api/export/csv` | — | `text/csv` download |
| POST | `/api/seed` | — | seed defaults if empty |

## 6. Calculation rules

- `Total loan payment = principal + interest`
- `Total expenses = living + child + rent + loan payment`
- `Net cash flow = salary − total expenses`
- Cumulative savings = starting savings + cumulative net cash flow
- Nov/Dec 2026: principal = fixed payment − bank interest
- Jan 2027 payoff: full remaining principal + payoff interest
- Entered but unconfirmed payoff interest is included as an estimate; January and subsequent cumulative savings are marked provisional.
- Missing payoff interest is treated as zero (excluded from the calculation); January and subsequent cumulative savings are marked provisional.
- Integer VND rounding; negative savings shown as shortfall

## 7. Acceptance criteria

1. 50 monthly rows + 5 annual rows (2026–2030).
2. Frontend edits persist to PostgreSQL and recalculate via API.
3. Nov/Dec principal from fixed payment − bank interest; Jan payoff = full principal.
4. Cumulative savings includes negative January payoff month.
5. Unconfirmed payoff interest visibly flagged provisional, with the UI explicitly stating whether an entered estimate is included or a missing amount is excluded.
6. Annual chart labels 2026–2030; 2026 marked partial.
7. Reload preserves inputs (from DB); reset restores defaults; CSV matches display.
8. The primary screen presents a dashboard with the existing forecast metrics and clear navigation to assumptions, annual reporting and monthly forecast details.
9. Navigation destinations map only to capabilities in this specification; the UI must not imply that transaction tracking, account/wallet management or budgeting are implemented.
10. The application shell and existing forecast views remain usable at desktop and narrow mobile viewport widths without horizontal page overflow.
11. The dashboard summarizes starting savings, starting/ending debt, projected debt payoff (when derivable), and annual cumulative-savings trajectory using forecast data only; it must not label savings as total assets or net worth.

## 8. Out of scope

- Bank sync, multi-user, cloud, investment/tax/inflation modeling, online deployment.
