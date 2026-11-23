# Household Authentication and Shared Finance Design

**Status:** Partially superseded on 2026-11-22. Invitation/shared-household behavior remains valid; registration and first-use workspace behavior are superseded by `specs/006-self-service-registration/spec.md`.
**Date:** 2026-11-21

## Intent

Add Login and Register to the existing local Personal Finance Planner. Every self-registered account owns an independent household financial plan. A household owner can still invite another person to collaborate on that same plan without requiring an email/SMTP service.

## Goals

- Individual registration, login, logout, and session lifecycle.
- Shared household data with distinct user identities and household membership.
- Every self-registered user becomes `OWNER` of a newly created household; invited registrants join the invitation's household as `MEMBER`.
- Owner can create a single-use invitation code/link with an expiry; the invitee can register with it.
- Enforce household isolation on every persisted financial read/write, including assumptions, expense categories/schedules, loans/repayments, reset/seed, and any future household-owned data.
- Preserve the existing financial dataset and forecast results through migration.
- Keep authentication isolated as a module/bounded context inside the existing Spring Boot modular monolith, with boundaries that do not prevent later extraction.
- Keep the current local Docker Compose deployment model.

## Non-goals

- Extracting authentication into a microservice now.
- SMTP, email delivery, password reset by email, social login, MFA, or external identity providers.
- Multiple household memberships per user in v1.
- Per-member permissions beyond `OWNER` and `MEMBER`.
- Bank synchronization, transaction tracking, payments, or cloud hosting.
- Exposing or accepting a client-selected `householdId` as an authorization mechanism.

## Approaches considered

### A. Shared household with invitation code (selected)

Each user has independent credentials. A `Household` owns financial data, and `HouseholdMember` connects users to it with a role. The owner generates a random one-time invitation code/link; the server stores only a hash and expiry. Registration consumes a valid code and joins the household. No email infrastructure is required.

**Trade-off:** The owner must securely share the code out-of-band. This avoids operational dependencies and is appropriate for a locally deployed family planner.

### B. Shared household with email invitations

The same household/member model, but invitations are delivered through a configured email provider.

**Trade-off:** Better recipient targeting and delivery workflow, but introduces SMTP/provider credentials, delivery failure handling, templates, and deployment configuration. It is unnecessary for the initial local use case.

### C. Separate financial workspace per account

Each user owns an independent dataset and can optionally copy data.

**Trade-off:** Supports independent plans but cannot replace invitation-based collaboration when family members intentionally share one plan.

**Updated decision (2026-11-22):** Use a hybrid of A and C. Self-registration creates a separate workspace; invitation registration joins the existing household. Email delivery (B) remains out of scope.

## Product behavior

### Registration and onboarding

1. Self-registration is always available, regardless of existing account count. Each request without an invitation creates a new household and an `OWNER` membership.
2. The legacy household created by migration V7 retains its existing financial records and existing membership; later self-registrations must never claim, copy, reset or reassign that data.
3. A valid invitation is consumed atomically with user creation and membership creation. The invitee becomes `MEMBER` of the inviter's household; a code cannot be reused, even if two registration requests race.
4. Each user has one household membership. A self-registered account never joins another account's household unless it registers with that household's invitation.
5. A new household's first assumptions read initializes a blank profile (income baseline `2026-11 = 0`, savings `0`, no expenses and no loans). Loading synthetic samples remains an explicit reset action.

### Login and sessions

- Users authenticate with a unique normalized email/username and password. The final identifier and validation rules are builder decisions consistent with existing UI conventions; prefer email as the login identifier without requiring email delivery.
- Passwords are stored only as a modern adaptive one-way hash using Spring Security's supported password encoder. Plaintext passwords are never logged or persisted.
- Use Spring Security with a server-side session and secure `HttpOnly` cookie; do not store bearer/session tokens in `localStorage`.
- Configure cookie `SameSite` and `Secure` attributes correctly for the actual local deployment topology. Do not assume HTTPS exists on localhost; document the production/TLS implication without claiming a production deployment exists.
- Protect state-changing requests against CSRF. Keep frontend/backend origin and CORS configuration explicit and restrictive for the current deployment.
- Logout invalidates the server-side session. Unauthenticated API calls return `401`; authenticated users lacking the required role receive `403`.
- Apply reasonable login throttling/attempt protections using a solution compatible with the existing deployment; do not leak whether an account exists in authentication error messages.

### Household membership and invitations

- `OWNER` and `MEMBER` share the household's financial data. Both can use normal financial CRUD operations.
- Only `OWNER` can create/revoke invitations or perform owner-only household administration.
- Invitation codes are generated with cryptographically secure randomness, stored as a hash, single-use, time-limited, and revocable. The raw code is shown only when created and is not written to logs.
- Expired, revoked, malformed, already-used, or unknown codes fail safely with a generic validation response.
- A member cannot elevate their role, change the household identity, or access another household by manipulating request parameters.
- v1 does not include removing members, transferring ownership, or multiple owners unless implementation discovers an unavoidable existing constraint; those are not implied by the initial scope.

## Data model and ownership

- `users`: unique normalized login identifier, password hash, timestamps, and account status.
- `households`: household identity and timestamps.
- `household_members`: user-to-household association and role, with a uniqueness constraint enforcing one household per user in v1.
- `household_invitations`: household, token hash, expiry, creation/revocation/consumption metadata; never store the raw token.
- Existing financial tables become household-owned: `assumptions`, `loans`, and `expense_categories` (and their dependent `loan_repayments` and `expense_amount_changes` through their parent relationships). Use database constraints and indexes to support household-scoped access and prevent cross-household references.

Independent registrations must be transactional. Concurrent requests with distinct normalized emails must both succeed and create distinct households; duplicate emails remain prevented by the database unique constraint. Invitation consumption remains protected by a row lock.

## Authorization and API boundaries

- Resolve the authenticated user and active household membership on the server for every request that reads or mutates household-owned data.
- Scope repository/service operations by the resolved household. Do not load a record globally and rely only on a later UI check.
- Ignore or reject client-supplied household identifiers for ordinary financial operations.
- Protect every existing financial endpoint, including assumptions, loans, forecast, CSV export, and seed/reset. Forecast and export must use only the authenticated household's persisted schedules/loans plus any explicitly supported preview payload; preview payloads must not permit reading or mutating another household.
- Seed/reset applies only to the caller's household and must be authorized deliberately; it must never reset all households or run automatically on login/registration.
- Add dedicated authentication and household invitation endpoints. Exact route names and DTO shape are builder decisions, but document the resulting API contract and status codes.

## Migration and compatibility

- Add forward-only Flyway migrations; never edit already-applied migrations.
- Preserve every existing financial row and its relationships. Migration V7 associates existing financial data with the legacy household. New self-registrations create separate households and do not claim that legacy household.
- Preserve current forecast totals and existing seed/reset semantics, but ensure all seeds are scoped to the caller's household.
- A fresh database must still initialize predictably. Any synthetic sample data belongs to the first household only and must not be silently copied into later households.
- Existing clients that call financial APIs without authentication will receive `401` after auth is enabled; update the frontend and E2E setup in the same release.
- Do not introduce a destructive database reset, `docker compose down -v`, or a migration that drops/recreates current financial data.

## Frontend experience

- Add a clear unauthenticated Login/Register experience and authenticated application shell.
- Login and self-registration are always available. Invitation links register members into the inviter's household; ordinary registration creates a new independent household.
- Owner UI exposes create/copy/revoke invitation functionality and clearly shows the one-time code/link and expiry. Do not display the raw token again after creation if it cannot be recovered from its hash.
- Provide logout and a minimal current-account/role indicator.
- Keep existing dashboard, assumptions, expenses, loans, forecast, and export workflows intact after login.
- On session expiry or `401`, return the user to login without discarding unrelated unsaved form state where practical.
- Maintain existing responsive behavior and visual conventions.

## Testing and acceptance criteria

1. Every self-registration creates exactly one user, one new household and an `OWNER` membership, including when other accounts already exist.
2. New household first-use assumptions and loans are blank; the owner can configure their own income, expenses, savings and loans.
3. Existing household records and memberships remain unchanged by new self-registrations.
4. Owner can generate an invitation; a valid code registers a second user into the same household as `MEMBER`.
5. Expired, revoked, invalid, and consumed invitations are rejected; concurrent attempts cannot consume the same invitation twice.
6. Passwords are hashed; login succeeds with valid credentials and fails safely with invalid credentials.
7. Session cookie is `HttpOnly`; logout invalidates the session; CSRF protection is verified for state-changing requests.
8. Both household members see the same assumptions, expenses, loans, and forecast, and permitted changes are visible to the other member.
9. A user in another household cannot read or modify the first household's data by guessing IDs or changing request payloads/URLs.
10. `MEMBER` cannot create/revoke invitations or perform owner-only actions; `OWNER` can.
11. Seed/reset and CSV export are household-scoped.
12. Unit, integration, and Playwright E2E tests cover authentication, onboarding, invitations, shared access, and cross-household isolation.
13. Existing repository quality gates pass, including backend unit/integration separation, coverage gates, frontend tests/build, and E2E.
14. Docker Compose deployment and restart behavior remain functional without committing secrets.

## Constraints and implementation calls left to the builder

- Keep the existing Spring Boot 3.3.5 / Java 21, Vue 3, PostgreSQL, Flyway, Maven, and Playwright stack; do not add a new identity service.
- Organize authentication, household membership, and financial ownership behind clear module/service boundaries. Avoid coupling the financial domain to Spring Security types; pass a resolved household context at application boundaries.
- Choose the exact login identifier normalization, password policy, session duration, invite expiry default, rate-limit implementation, and API route names. Document the choices and test them.
- Ensure the initial-household claim and invite consumption are transactional and protected against races.
- Preserve the repository's testing rules: integration tests use H2 in-memory and must not connect to local/production databases; do not leak secrets in logs or test fixtures.
- Update the product specification and developer documentation to remove the single-user/no-sign-in limitation and describe bootstrap, invitation, local session/security configuration, and recovery/operational considerations.

## Approval boundary

This document defines the intended product behavior and security boundaries. No product code, dependencies, schema migrations, or scaffolding should be changed until the user approves this design. After approval, create a detailed implementation plan and execute it with Superpowers TDD and verification workflow.
