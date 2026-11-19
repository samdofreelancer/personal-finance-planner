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
4. Maintain multiple loans and manually add, edit and remove dated repayment entries for each loan.
5. View monthly net cash flow and cumulative savings/shortfall.
6. Compare annual net cash flow 2026–2030 in chart and table.
7. Distinguish the partial year (2026) from full forecast years.
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
| Sample loan opening principal | 120,000,000 |
| Sample repayment 11/2026 | 8,800,000 principal + 1,200,000 interest |
| Sample repayment 12/2026 | 8,900,000 principal + 1,100,000 interest |
| Sample repayment 01/2027 | 102,300,000 principal + 1,000,000 interest |

## 5. API Contract

| Method | Endpoint | Request | Response |
|--------|----------|---------|----------|
| GET | `/api/assumptions` | — | `AssumptionDTO` |
| PUT | `/api/assumptions` | `AssumptionDTO` | `AssumptionDTO` |
| GET | `/api/loans` | — | `LoanDTO[]` |
| PUT | `/api/loans` | Complete `LoanDTO[]` collection | Replaces the loan collection atomically; supplied IDs update existing records, omitted records are deleted | `LoanDTO[]` |
| GET | `/api/forecast` | query params | `ForecastResponse { months[], annual[] }` |
| GET | `/api/export/csv` | — | `text/csv` download |
| POST | `/api/assumptions/seed` | — | Reset assumptions and sample loan schedule |

## 6. Calculation rules

- `Total loan payment = principal + interest`
- `Total expenses = living + child + rent + loan payment`
- `Net cash flow = salary − total expenses`
- Cumulative savings = starting savings + cumulative net cash flow
- Each repayment contributes its scheduled principal and interest to the month containing its due date.
- Multiple entries in the same month are aggregated across all loans.
- Applied principal is capped at each loan's remaining principal; interest is included as entered.
- Forecast and CSV read saved loan schedules from PostgreSQL; other editable assumptions remain query-bound preview inputs.
- Integer VND rounding; negative savings shown as shortfall

## 7. Acceptance criteria

1. 50 monthly rows + 5 annual rows (2026–2030).
2. Assumptions and loan schedules persist to PostgreSQL; reload restores both.
3. Multiple loans and arbitrary dated repayment entries can be created, updated and deleted.
4. Multiple entries in one month aggregate principal, interest and total payment correctly.
5. Entries outside the forecast horizon remain saved and do not affect visible forecast months.
6. Cumulative savings includes scheduled repayments and retains negative shortfalls.
7. Annual chart labels 2026–2030; 2026 marked partial.
8. Reset restores sample assumptions and an equivalent sample loan schedule; CSV matches the displayed forecast.
9. The primary screen presents a dashboard with clear navigation to assumptions, loan management, annual reporting and monthly forecast details.
10. Navigation destinations map only to capabilities in this specification; the UI must not imply that transaction tracking, account/wallet management or budgeting are implemented.
11. The application shell and existing forecast views remain usable at desktop and narrow mobile viewport widths without horizontal page overflow.
12. The dashboard summarizes starting savings, total starting/ending debt, projected debt payoff (when derivable), and annual cumulative-savings trajectory using forecast data only; it must not label savings as total assets or net worth.

## 8. Out of scope

- Bank sync, multi-user, cloud, investment/tax/inflation modeling, online deployment.
