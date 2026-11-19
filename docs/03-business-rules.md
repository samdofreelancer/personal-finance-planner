# 03. Business Rules

This document records the behavior implemented in `ForecastService` and its DTOs. The rules are a snapshot of the current implementation, not financial advice. Update tests and this document together when changing a rule.

## Forecast window and aggregation

- Forecast starts at **Nov 2026** and contains **50 monthly periods**, ending **Dec 2030**.
- Annual summary contains 2026–2030 (5 rows).
- 2026 is a partial year containing Nov and Dec only.
- Currency is VND; current persisted/API monetary fields are integer VND (`long`/`Long`).

## Income

- Base monthly salary is adjusted by annual raises from April of the corresponding year.
- Raise factors compound across eligible years.
- January salary is multiplied by `januaryMultiplier` after raise factors are applied.
- Current implementation computes the salary factor with binary floating-point and rounds to whole VND using `HALF_UP`. This is a known correctness gap; see remediation finding F-03.

## Expenses

- Monthly expenses include living, child, rent and loan payment.
- Child amount:
  - 2026: `childThrough2026`.
  - Jan–Mar 2027: `childQ12027`.
  - Apr 2027 onward: `childFromApr2027`.
- Rent amount:
  - Through Jan 2027: `rentThroughJan2027`.
  - Feb 2027 onward: `rentFromFeb2027`.

## Loan repayment schedule

- Each loan has a name, opening principal and zero or more dated repayment entries.
- Each entry contains a due date, non-negative whole-VND principal and non-negative whole-VND interest.
- All entries due in a forecast month are aggregated across loans.
- Principal is applied sequentially per loan and capped at that loan's remaining balance; interest is included as entered.
- A repayment outside Nov 2026–Dec 2030 remains persisted and is excluded from the current forecast window.
- There is no paid/unpaid ledger or automatic amortization; entries are planned forecast inputs.
- The legacy Nov/Dec fixed payments and Jan payoff have been migrated to equivalent dated entries. They are no longer calculation rules.

## Cash flow and savings

- `totalExpenses = living + child + rent + loanPayment`.
- `netCashFlow = salary - totalExpenses`.
- `cumulativeSavings = startingSavings + cumulative monthly netCashFlow`.
- Negative cumulative savings are retained as negative values (shortfall), not clamped to zero.
- Forecast provisional flags remain part of the response contract for future use; manually entered repayment entries are treated as the user's planned values and are not marked provisional.

## Validation constraints (current)

- Money inputs are expected to be non-negative whole VND.
- January multiplier must be greater than zero and no more than 20.
- Annual raise rates for 2027–2030 must be between 0% and 100%.
- Each loan name must contain 1–120 characters.
- Opening principal, repayment principal and repayment interest must be non-negative whole VND values.
- Every repayment must have a valid calendar date.

Backend validation is authoritative for API requests. Keep client validation aligned, but never rely on it alone.

## Change-control requirements

Any change to these rules must include:

1. A requirement/spec update describing the intended behavior.
2. Backend tests for the changed rule and relevant boundaries.
3. Baseline comparison to detect unintended output changes.
4. Updates to this document and API/UX documentation if observable behavior changes.
