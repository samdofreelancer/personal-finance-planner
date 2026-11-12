# Feature Specification: Personal Finance Planner (2026–2030)

**Status:** Implemented and verified  
**Target:** Local web application; current workbook values are used as editable startup assumptions.

## 1. Purpose

Provide a private, easy-to-edit financial forecast for November 2026 through December 2030. The user can change income, annual raises, expenses, loan details, and starting savings, then immediately see monthly and annual cash-flow outcomes and savings progress.

## 2. Users and scope

- Single household user; no account or sign-in.
- Runs locally in a web browser on the user's computer.
- Forecast and editable assumptions are stored on that device only.
- No bank connection, cloud sync, payments, or financial advice.

## 3. User stories

1. As a user, I can edit financial assumptions and see the monthly forecast recalculate.
2. As a user, I can change each April raise independently, including entering zero or a custom percentage.
3. As a user, I can enter a different January salary multiplier without changing other months.
4. As a user, I can enter bank-reported interest for the November and December 2026 payments and the January 2027 payoff.
5. As a user, I can see each month's net cash flow and cumulative savings/shortfall.
6. As a user, I can compare annual net cash flow from 2026 through 2030 in a chart and table.
7. As a user, I can distinguish a partial year from a full year and recognize when the payoff interest is unconfirmed.
8. As a user, I can reset assumptions to their initial values and export a copy of my plan.

## 4. Initial assumptions

All monetary values are VND. The following are based on the current workbook unless noted:

| Assumption | Initial value |
|---|---:|
| Forecast period | Nov 2026–Dec 2030 |
| 2026 base monthly salary | 50,000,000 |
| Raise starting April 2027, 2028, 2029, 2030 | 10% each year, editable independently |
| January salary multiplier | 2.5×, matching the current workbook; editable |
| Living expenses | 3,000,000/month |
| Child fund through Dec 2026 | 5,000,000/month |
| Child fund Jan–Mar 2027 | 6,000,000/month |
| Child fund from Apr 2027 | 7,000,000/month |
| Rent through Jan 2027 | 5,000,000/month |
| Rent from Feb 2027 | 8,000,000/month |
| Starting savings before Nov 2026 | 220,000,000 |
| Loan principal on 28 Oct 2026 | 368,261,629 |
| Fixed Nov and Dec 2026 loan payments | 20,570,000/month, principal plus interest |
| Bank-reported interest, 30 Nov 2026 | 3,565,230 |
| Bank-reported interest, 28 Dec 2026 | 3,756,588 |
| Expected payoff date | 25 Jan 2027 |
| Payoff interest currently in workbook | 3,683,193; shown as current workbook value, editable or clearable if unconfirmed |

### Source-of-truth note

- Startup defaults match the values currently saved in the workbook, including the 2.5× January multiplier and 3,683,193 VND payoff-interest entry. These are editable and are not represented as bank-verified unless the user confirms them.
- The original request mentioned a 2× January default. The current workbook contains 2.5×; the app follows the workbook as the latest saved plan.
- The workbook has a loan annual interest rate of 13.2%, but the November/December interest amounts are entered from bank statements. The app does not replace observed figures with a rate-based estimate.

## 5. Functional requirements

### 5.1 Assumptions

- Show editable inputs grouped into income, expenses, savings, and loan sections.
- Accept non-negative VND amounts and raise percentages from 0% through 100%; validate and explain invalid entries.
- Let the user independently edit raise percentages for April 2027–2030.
- Let the user freely edit the January salary multiplier; the initial value is 2.5× to match the latest workbook.
- Let the user edit actual interest for each of the three loan events.
- Clearly identify the January payoff-interest field as requiring a bank-confirmed value. If not confirmed, mark affected cash-flow and savings projections as provisional; do not silently treat missing interest as a confirmed zero.
- Provide reset-to-initial-values with an explicit confirmation.
- Persist assumptions locally across page reloads. Provide an explicit reset and export so data remains under user control.

### 5.2 Monthly forecast

Generate one row for each month from November 2026 through December 2030, inclusive. Show:

- Month
- Salary
- Living expenses
- Child fund
- Rent
- Opening loan principal
- Principal paid
- Interest paid
- Total loan payment
- Total expenses
- Net cash flow
- Cumulative savings or shortfall

Apply salary raises cumulatively beginning in April of each specified year. January salary is the raised base monthly salary for that year multiplied by the January multiplier.

### 5.3 Loan calculation

- November and December 2026 total payment equals the editable fixed monthly payment.
- For each of these payments, principal paid is `min(opening principal, max(0, fixed payment - bank-reported interest))`.
- January 2027 principal paid equals the outstanding principal after the December payment; no equal-installment schedule is introduced.
- January total payoff equals outstanding principal plus the manually entered payoff interest. The payoff date is 25 January 2027.
- After payoff, loan balance, principal paid, interest paid, and payment are zero.
- The 13.2% annual rate is retained as a reference assumption only unless a future, separately approved feature defines a bank-verified daily interest method.

### 5.4 Cash flow and savings

- `Total loan payment = principal paid + interest paid`.
- `Total expenses = living expenses + child fund + rent + total loan payment`.
- `Net cash flow = salary - total expenses`.
- First month's closing savings = starting savings + November 2026 net cash flow.
- Each following month's closing savings = prior month's closing savings + current month's net cash flow.
- Negative closing savings are shown as a shortfall, not clamped to zero.
- Clearly label monthly net cash flow separately from cumulative savings; the initial savings balance is not income.

### 5.5 Annual summary and chart

- Summarize total salary, total expenses, net cash flow, and year-end cumulative savings/shortfall for each calendar year.
- 2026 is a partial year containing November and December only; label it as partial.
- Show an annual net-cash-flow column chart for 2026–2030 with actual year labels, a descriptive title, VND units, and a zero baseline so negative years are apparent.
- Keep the annual summary table visible alongside or directly below the chart.
- Recalculate chart and summary whenever assumptions change.

### 5.6 Export and privacy

- Allow export of the assumptions and monthly/annual forecast as CSV.
- Do not transmit financial data outside the local browser.
- Do not include analytics, remote fonts, or third-party scripts that send data over the network.

## 6. UX and accessibility

- Vietnamese-language interface; use clear VND number formatting and month/year labels.
- Layout adapts to desktop and mobile widths.
- Label every input and provide contextual validation errors.
- Do not use color alone to communicate surplus versus shortfall; include a sign and text label.
- Provide keyboard-accessible inputs, controls, tables, and chart description/summary.

## 7. Acceptance criteria

1. With the initial assumptions, the forecast contains exactly 50 monthly rows (Nov 2026–Dec 2030).
2. Changing any raise, expense, loan-interest, salary multiplier, or starting-savings input updates dependent monthly and annual results without reloading.
3. Monthly loan principal for Nov/Dec is derived from the fixed payment minus the entered bank interest; the January payoff uses the full remaining principal, not an installment split across months.
4. Cumulative savings equals starting savings plus the cumulative net cash flow, including the negative January payoff month.
5. A missing/unconfirmed payoff-interest amount is visibly flagged and cannot be mistaken for an official, complete January estimate.
6. The annual chart uses 2026–2030 labels and net cash flow values matching the annual table; 2026 is identified as partial.
7. A page reload preserves local inputs; reset restores the approved defaults; CSV export contains the same displayed forecast values.
8. All calculations are deterministic and use integer VND rounding consistently; rounding rules are documented in the implementation plan.

## 8. Out of scope

- Bank integrations, automatic balance synchronization, or interest estimation from the nominal annual rate.
- Investment-return, inflation, tax, insurance, or education-cost modeling.
- Multi-user accounts, cloud storage, shared plans, or an online deployment.
- Recommendations about borrowing, investments, or financial products.
