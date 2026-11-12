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

## Loan payoff

- Nov 2026 interest is `novInterest`; principal paid is `min(openingPrincipal, max(0, fixedPayment - interest))`.
- Dec 2026 uses the same rule with `decInterest`.
- Jan 2027 pays the entire remaining opening principal plus payoff interest.
- When payoff interest is absent or not confirmed, January is marked provisional. If the amount is null, current calculation uses zero interest while marking the result provisional.
- No further loan payment is modeled after Jan 2027 in the current forecast window.

## Cash flow and savings

- `totalExpenses = living + child + rent + loanPayment`.
- `netCashFlow = salary - totalExpenses`.
- `cumulativeSavings = startingSavings + cumulative monthly netCashFlow`.
- Negative cumulative savings are retained as negative values (shortfall), not clamped to zero.
- Provisional status propagates to subsequent cumulative savings after a provisional month.

## Validation constraints (current)

- Money inputs are expected to be non-negative whole VND.
- January multiplier must be greater than zero and no more than 20.
- Annual raise rates for 2027–2030 must be between 0% and 100%.
- Nov/Dec interest cannot exceed the fixed payment.
- Payoff interest must be present if the confirmation flag is true.

Backend validation is authoritative for API requests. Keep client validation aligned, but never rely on it alone.

## Change-control requirements

Any change to these rules must include:

1. A requirement/spec update describing the intended behavior.
2. Backend tests for the changed rule and relevant boundaries.
3. Baseline comparison to detect unintended output changes.
4. Updates to this document and API/UX documentation if observable behavior changes.
