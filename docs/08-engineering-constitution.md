# 08. Engineering Constitution

These principles are the default constraints for all changes. A change that needs to violate one must explain why and record an explicit decision; agents must not silently weaken these rules.

## 1. Product integrity

- Build for the documented single-household, local-first planning use case.
- Do not add multi-user, bank-sync, cloud, investment-advice or unrelated Financial GPS capabilities without an accepted specification.
- Keep demo assumptions visibly distinct from real user-entered data.

## 2. Financial correctness is a safety property

- Treat financial calculations as domain behavior, not presentation logic.
- Use deterministic decimal arithmetic for monetary values; avoid binary floating-point for money.
- Define rounding, precision, currency unit, effective date and month-boundary behavior explicitly.
- Test normal cases, zero/negative boundaries where valid, maximum/minimum inputs, month/year transitions and payoff/overflow cases.
- Never silently change an established financial rule to make a test pass. Update the specification first.

## 3. Clear boundaries and explicit contracts

- Keep UI concerns in the frontend and financial/business calculations in the backend domain/service layer.
- Controllers translate and validate transport input; they should not become the home of business rules.
- Treat API request/response shapes, defaults, error behavior and side effects as versionable contracts.
- Distinguish saved assumptions from unsaved simulation inputs in names, API semantics and UI state.
- Avoid hidden coupling between persistence and forecast computation.

## 4. Persistence and data lifecycle

- Persist only data needed by the documented product behavior.
- Any schema change must include a safe, reviewable evolution path; do not rely on an undocumented production schema mutation.
- Protect against lost updates and out-of-order autosave writes when concurrent requests are possible.
- Destructive operations and database-volume deletion must be explicit and documented.
- Do not log financial payloads or expose them through diagnostics unnecessarily.

## 5. Privacy and security by default

- Never commit secrets, real financial records, credentials, tokens or personal identifiers.
- Keep database access and internal service ports off untrusted networks by default.
- Validate all externally supplied values on the backend; frontend validation is usability support, not a security boundary.
- Apply least privilege and avoid adding authentication/security claims until actually implemented and verified.
- Treat CSV files, database backups and screenshots as sensitive household data.

## 6. Test-driven change discipline

- For behavior changes, write or update a failing test before implementation where practical.
- Prefer tests at the lowest useful layer, plus integration/contract tests for important cross-layer behavior.
- Tests must assert business outcomes and edge cases, not merely that methods were called.
- A green build with zero tests is not evidence of behavioral correctness; report the test gap honestly.
- Do not weaken, delete or skip a meaningful test merely to obtain a passing build.

## 7. Maintainability over cleverness

- Prefer the simplest design that preserves explicit domain rules and clear boundaries.
- Avoid speculative abstractions, premature microservices and unnecessary dependencies.
- Keep names aligned with domain language; document non-obvious decisions rather than relying on tribal knowledge.
- Make changes small, reviewable and reversible where possible.

## 8. Documentation and traceability

- Every non-trivial change must trace to an accepted requirement or an explicitly recorded maintenance/security rationale.
- Update business rules, API contract, data model, architecture and diagrams when they change.
- Keep `as-is`, `to-be`, decisions and open findings distinct.
- Diagrams must describe the actual system and must have editable source committed with the docs.

## 9. Definition of done

A change is done only when all applicable items are satisfied:

- [ ] Scope and acceptance criteria are clear.
- [ ] Implementation matches the accepted specification.
- [ ] Relevant automated tests pass; missing coverage is called out.
- [ ] Frontend/backend builds and relevant integration checks pass.
- [ ] Financial precision, timing and boundary behavior are verified where applicable.
- [ ] API compatibility, persistence, concurrency, privacy and security impacts are reviewed.
- [ ] Documentation and diagram sources are current.
- [ ] No secrets or real personal financial data were introduced.
- [ ] Final report records evidence, known gaps and actual Git state.

