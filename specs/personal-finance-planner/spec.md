# Feature Specification: Personal Finance Planner (Configurable Forecast)

**Status:** Active baseline — authenticated, household-scoped planner
**Stack:** Spring Boot (Java 21) backend + PostgreSQL database + Vue.js frontend  
**Communication:** REST API (frontend → backend)

## 1. Purpose

Personal financial forecast for a configurable planning period. User edits effective-month income amounts, expenses, loan details, and starting savings; sees monthly and annual cash-flow outcomes and savings progress. Reset-to-sample values are synthetic demonstration data. New households start with blank financial inputs and configure their own data.

## 2. Users and scope

- Each self-registered account owns an independent household workspace; invited members can collaborate in that household. See `specs/006-self-service-registration/spec.md`.
- Login and self-registration are always available. Self-registration creates a new OWNER household; invitation registration joins an existing household as MEMBER.
- Local deployment: Spring Boot backend + PostgreSQL + Vue.js frontend.
- Income assumptions, recurring expense schedules and loan schedules persist in PostgreSQL; forecast rows are calculated on demand.
- No bank sync, cloud, payments, or financial advice.

## 3. User stories

1. Edit financial assumptions via Vue; forecast recalculates via Spring Boot REST API.
2. Configure explicit monthly income amounts and their effective months; no percentage growth is assumed.
3. Change January salary multiplier independently.
4. Maintain multiple loans and manually add, edit and remove dated repayment entries for each loan.
5. Create, rename and remove arbitrary recurring expense categories.
6. Define one or more monthly amount changes for each expense category, each effective from a selected month.
7. View monthly net cash flow and cumulative savings/shortfall.
8. Compare annual net cash flow across the configured forecast horizon (default 2026–2030) in chart and table.
9. Distinguish the partial year (2026) from full forecast years.
10. Reset to defaults; export CSV.

## 4. Sample assumptions (VND)

| Assumption | Value |
|---|---:|
| Default forecast period | Nov 2026–Dec 2030 (4-year setting) |
| Default income schedule | 11/2026: 42,000,000; 04/2027: 45,360,000; 04/2028: 48,988,800; 04/2029: 52,907,904; 04/2030–04/2032: 57,140,536/month |
| January salary multiplier | 2.0×, editable |
| Initial living expense seed | 4,250,000/month |
| Initial child fund seed, first period | 2,500,000/month |
| Initial child fund seed, second period | 2,750,000/month |
| Initial child fund seed, later period | 3,250,000/month |
| Initial rent seed, first period | 3,750,000/month |
| Initial rent seed, later period | 5,750,000/month |
| Starting savings | 30,000,000 |
| Sample loan opening principal | 120,000,000 |
| Sample repayment 11/2026 | 8,800,000 principal + 1,200,000 interest |
| Sample repayment 12/2026 | 8,900,000 principal + 1,100,000 interest |
| Sample repayment 01/2027 | 102,300,000 principal + 1,000,000 interest |

The sample expense categories are editable seed data, not a fixed list of supported expense types. The initial schedules are:

| Category | Effective month | Monthly amount (VND) |
|---|---:|---:|
| Sinh hoạt phí | 11/2026 | 4,250,000 |
| Quỹ nuôi con | 11/2026 | 2,500,000 |
| Quỹ nuôi con | 01/2027 | 2,750,000 |
| Quỹ nuôi con | 04/2027 | 3,250,000 |
| Tiền nhà | 11/2026 | 3,750,000 |
| Tiền nhà | 02/2027 | 5,750,000 |

## 5. API Contract

| Method | Endpoint | Request | Response |
|--------|----------|---------|----------|
| GET | `/api/auth/bootstrap` | — | `registrationAvailable: true` |
| POST | `/api/auth/register` | `{ email, password }` or `{ email, password, invitationToken }` | `AuthResponse` + authenticated session |
| POST | `/api/auth/login` | `{ email, password }` | `AuthResponse` + authenticated session |
| POST | `/api/auth/logout` | Session + CSRF token | `204 No Content` |
| GET | `/api/auth/me` | Session | Current `AuthResponse` |
| GET | `/api/assumptions` | — | `AssumptionDTO` |
| PUT | `/api/assumptions` | `AssumptionDTO` | `AssumptionDTO` |
| GET | `/api/loans` | — | `LoanDTO[]` |
| PUT | `/api/loans` | Complete `LoanDTO[]` collection | Replaces the loan collection atomically; supplied IDs update existing records, omitted records are deleted | `LoanDTO[]` |
| POST | `/api/forecast` | JSON assumptions snapshot including `expenses[]` | `ForecastResponse { months[], annual[] }` |
| POST | `/api/export/csv` | JSON assumptions snapshot including `expenses[]` | `text/csv` download |
| POST | `/api/assumptions/seed` | — | Reset assumptions and sample loan schedule |

## 6. Calculation rules

- `Total loan payment = principal + interest`
- For each recurring expense category and forecast month, use the amount from the latest effective month not later than the forecast month; use zero before the first effective month.
- `Total expenses = sum(recurring expense amounts) + loan payment`
- `Net cash flow = salary − total expenses`
- Cumulative savings = starting savings + cumulative net cash flow
- Each repayment contributes its scheduled principal and interest to the month containing its due date.
- Multiple entries in the same month are aggregated across all loans.
- Applied principal is capped at each loan's remaining principal; interest is included as entered.
- Forecast and CSV accept a complete assumptions snapshot in JSON for preview; loan schedules are loaded from PostgreSQL.
- Setting an effective monthly amount to zero stops that recurring expense from that month onward.
- Integer VND rounding; negative savings shown as shortfall

## 7. Acceptance criteria

1. The default 4-year horizon produces 50 monthly rows + 5 annual rows (2026–2030); configurable horizons are specified in `specs/004-forecast-horizon/spec.md`.
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
13. Users can add, rename and delete expense categories without code or schema changes.
14. Each expense category has at least one unique `YYYY-MM` effective month and a non-negative whole-VND monthly amount; duplicate category names are rejected case-insensitively.
15. Expense changes take effect at the beginning of their selected month and persist across reloads.
16. Existing living, child and rent values are migrated to equivalent expense categories and effective-month schedules without changing forecast totals.
17. Monthly forecast and CSV show the aggregate recurring expense amount and its category breakdown.
18. Income schedule requires a 11/2026 baseline and supports multiple unique future effective months; each month uses the latest effective amount.
19. January multiplier is applied to the effective monthly income amount; no automatic percentage raise or income extrapolation is performed.
20. Income changes persist in PostgreSQL and remain available when the forecast horizon is shortened.
21. Every successful self-registration creates a distinct household, regardless of how many users already exist.
22. A new household starts with zero income baseline and savings, no expense categories and no loan schedules; reset-to-sample remains an explicit action.
23. Existing household financial data is not copied, reassigned or overwritten by later registrations.

## 8. Out of scope

- Bank sync, multiple household memberships per user, cloud, investment/tax/inflation modeling, online deployment.
