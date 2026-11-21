# Household Authentication and Shared Finance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add individual accounts and secure shared-household access to the existing financial plan, with owner-generated single-use invitations and no email dependency.

**Architecture:** Keep Spring Boot as a modular monolith. Add an authentication/identity boundary for users, sessions and invitations, a household membership boundary, and pass a resolved household context into financial application services. Household ownership is enforced in database constraints and scoped repository operations; Vue uses same-origin session cookies and a login/onboarding shell.

**Tech Stack:** Spring Boot 3.3.5, Java 21, Spring Security, Spring Data JPA, PostgreSQL 16, Flyway, Vue 3, Vitest, Playwright, Docker Compose.

**Spec:** `docs/superpowers/specs/2026-11-21-household-authentication-design.md`

## Global Constraints

- Keep the existing Spring Boot 3.3.5 / Java 21, Vue 3, PostgreSQL, Flyway, Maven, and Playwright stack; do not add a new identity service.
- Add forward-only Flyway migrations; never edit already-applied migrations.
- Preserve every existing financial row and its relationships. Create the initial household and associate all current financial data with it as part of the migration.
- Backend integration tests must use an isolated in-memory database (H2) and must never connect to a developer or production database.
- Do not reset, seed or overwrite household assumptions as part of verification without explicit authorization; seed data changes persisted state.
- Do not use `docker compose down -v` unless the user explicitly intends to delete the persisted database volume.
- Do not commit secrets, credentials, tokens or real financial data.
- Do not create commits, push, merge or deploy unless the user explicitly requests it; keep the implementation on its isolated feature branch until then.
- Keep backend `mvn verify` required in both independent backend CI jobs; preserve coverage gates (80% line, 70% branch), frontend gates (80% line/statement, 70% function, 75% branch), and PIT's 80% threshold for its current targets.
- Local Compose is the documented deployment model; do not infer or claim a production/cloud deployment procedure exists.

## Review Focus

- Concurrent first registrations must not create two initial households; test simultaneous bootstrap attempts and assert exactly one owner/household succeeds.
- Invitation reuse/race, expiry and revocation must never create duplicate memberships; test simultaneous consumption and invalid token states.
- Guessing another household's entity IDs or supplying forged household IDs must not permit read/write/delete; test assumptions, loans and expenses at API boundaries.
- Existing database migration must preserve assumptions, loan repayments and expense schedules exactly; test V1/V4 legacy migration through the new migration using a PostgreSQL-compatible H2 fixture and a dedicated migration verification path.
- Session/CSRF behavior must work with the real same-origin Nginx frontend and remain safe across login/logout; test cookie flags, CSRF rejection and browser navigation.

---

## File Map

### Backend — authentication and household modules

- Create `backend/src/main/java/com/finance/planner/auth/` for user identity, password handling, session configuration, authentication endpoints and request identity resolution.
- Create `backend/src/main/java/com/finance/planner/household/` for household/member/invitation entities, repositories, services and endpoints.
- Create `backend/src/main/java/com/finance/planner/security/` for Spring Security filter-chain, CSRF, session and authorization configuration.
- Modify `backend/pom.xml` to add Spring Security and Spring Security test dependencies only when their tests are introduced.
- Modify `backend/src/main/resources/application.properties` and Compose environment as required for session cookie/security configuration; no secret values committed.

### Backend — financial ownership

- Modify `backend/src/main/java/com/finance/planner/entity/Assumption.java`, `Loan.java`, and `ExpenseCategory.java` to associate each aggregate root with a household.
- Modify `backend/src/main/java/com/finance/planner/repository/AssumptionRepository.java`, `LoanRepository.java`, and `ExpenseCategoryRepository.java` to require household-scoped operations.
- Modify `backend/src/main/java/com/finance/planner/service/AssumptionService.java`, `LoanScheduleService.java`, and `ExpenseScheduleService.java` to accept resolved household context and scope every operation.
- Modify `backend/src/main/java/com/finance/planner/controller/AssumptionController.java`, `LoanController.java`, and `ForecastController.java` to use authenticated household context, including seed/reset, forecast and CSV.
- Add Flyway `V7__add_household_authentication.sql` (or next unused version after checking current migration inventory) for users, households, memberships, invitations and household ownership columns/constraints. Never alter V1–V4.

### Frontend

- Create `frontend/src/services/auth.js` for register/login/logout/session/invitation API calls.
- Create `frontend/src/components/AuthView.vue` for login and initial/invitation registration.
- Create `frontend/src/components/HouseholdSettings.vue` for owner invitation creation/revocation and role/account display.
- Modify `frontend/src/services/api.js` to use same-origin credentials by default, include CSRF tokens on unsafe methods and centralize `401` handling.
- Modify `frontend/src/App.vue` and `frontend/src/assets/style.css` for unauthenticated/authenticated states, owner invitation settings and logout while preserving existing financial views.

### Tests and documentation

- Backend unit tests under `backend/src/test/java/com/finance/planner/auth/`, `household/`, `security/`, plus financial service test updates.
- Backend integration tests under `backend/src/test/java/com/finance/planner/integration/` using the existing `*IT` Failsafe convention and H2 only.
- Add `HouseholdIsolationIT.java` to verify two-household data isolation, forged household fields, foreign aggregate IDs, forecast/CSV and scoped seed/reset behavior.
- Frontend Vitest tests under `frontend/src/**/__tests__/` or existing test conventions discovered during implementation.
- Playwright page objects and specs under `e2e/pages/` and `e2e/tests/`; update `e2e/fixtures/test.js`, `e2e/scripts/run-e2e.mjs`, `docker-compose.e2e.yml`, and CI only where required for isolated multi-user scenarios.
- Update `specs/personal-finance-planner/spec.md`, `docs/README.md`, architecture/API/data/business docs linked there, `docs/06-development-guide.md`, and editable architecture diagram sources.

## Task 1: Add authentication and security dependencies/configuration

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/finance/planner/security/SecurityConfiguration.java`
- Create: `backend/src/main/java/com/finance/planner/security/CsrfTokenController.java` (or equivalent endpoint if the chosen SPA integration requires token bootstrap)
- Create: `backend/src/test/java/com/finance/planner/security/SecurityConfigurationTest.java`
- Modify: `backend/src/test/java/com/finance/planner/PlannerApiIT.java`

**Interfaces:**
- Produces a Spring Security filter chain with session authentication, CSRF protection, explicit public auth endpoints, and all other `/api/**` routes authenticated by default.
- Provides a same-origin CSRF token bootstrap contract for Vue; unsafe requests require a valid token.

- [x] **Step 1: Write failing security tests** asserting unauthenticated financial API calls return `401`, public auth/bootstrap endpoints remain accessible, unsafe unauthenticated calls do not mutate data, and an authenticated request without CSRF token is rejected.
- [x] **Step 2: Run the targeted tests and verify RED** because the security filter chain and dependencies are not present.
- [x] **Step 3: Add Spring Security dependencies and minimal filter-chain configuration** using server-side sessions, CSRF cookie/header conventions suitable for same-origin Vue/Nginx, and deny-by-default API authentication.
- [x] **Step 4: Run targeted security tests and verify GREEN**, then run the existing backend integration suite to identify test clients that must authenticate explicitly.
- [x] **Step 5: Keep changes uncommitted on the isolated feature branch** until the user explicitly requests a commit.

## Task 2: Create users, households, membership and invitations

**Files:**
- Create: `backend/src/main/java/com/finance/planner/auth/User.java`, `UserRepository.java`, `AuthService.java`, `AuthController.java`, request/response DTOs and password encoder configuration.
- Create: `backend/src/main/java/com/finance/planner/household/Household.java`, `HouseholdMember.java`, `HouseholdInvitation.java`, repositories, `HouseholdService.java`, `InvitationService.java`, `HouseholdController.java`, invitation DTOs.
- Create: `backend/src/test/java/com/finance/planner/auth/AuthServiceTest.java`.
- Create: `backend/src/test/java/com/finance/planner/household/InvitationServiceTest.java`.
- Create: `backend/src/test/java/com/finance/planner/household/HouseholdBootstrapTest.java`.

**Interfaces:**
- `AuthService.registerInitial(RegisterRequest): AuthResponse` creates the first user/household/OWNER transactionally only when no user exists.
- `AuthService.registerWithInvitation(RegisterRequest, String): AuthResponse` consumes a valid invite atomically and creates a MEMBER.
- `AuthService.authenticate(LoginRequest): AuthenticatedUser` validates normalized login identifier/password.
- `InvitationService.createInvitation(HouseholdContext, Duration): CreatedInvitation` returns the raw token once; persists only its cryptographic hash.
- `InvitationService.revoke(HouseholdContext, Long): void` revokes only an invitation in the caller's household using a pessimistic row lock.
- `HouseholdContext` is an immutable application-level value containing household ID, user ID and role; financial domain services do not depend on Spring Security principal types.

- [x] **Step 1: Write failing unit tests** for normalized unique login identifiers, adaptive password hashing, first-user bootstrap, second-user invite registration, invalid/expired/revoked/consumed token rejection, owner-only invite actions, and concurrent first-registration/invite-consumption behavior.
- [x] **Step 2: Run targeted tests and verify RED** for missing auth/household types and behaviors.
- [x] **Step 3: Implement entities, repositories and transactional services** with database-enforced uniqueness, cryptographically random invite tokens, hash-only persistence, one household membership per user, and race-safe bootstrap/consume operations.
- [x] **Step 4: Run targeted tests and verify GREEN**; include a real database concurrency test in the H2 integration layer if an in-memory unit test cannot prove the transaction constraint.
- [x] **Step 5: Keep changes uncommitted** on the isolated feature branch until the user explicitly requests a commit.

## Task 3: Add the data-preserving household migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__add_household_authentication.sql` (confirm the next unused version before naming).
- Create: `backend/src/test/java/com/finance/planner/integration/HouseholdMigrationIT.java`.
- Keep: `backend/src/test/java/com/finance/planner/integration/ExpenseMigrationIT.java`; do not weaken its assertions.

**Interfaces:**
- Migration creates the user/household/member/invitation schema and household ownership columns for `assumptions`, `loans`, and `expense_categories`.
- Child rows (`loan_repayments`, `expense_amount_changes`) remain scoped through their household-owned parent aggregate; add constraints/indexes needed to prevent invalid cross-household parent relationships.
- Existing rows are associated with one initial household without inventing a real user or storing credentials in the migration.

- [x] **Step 1: Write failing migration integration tests** using synthetic legacy data to assert all original assumption values, loan and repayment rows, expense categories and effective-month amounts remain byte/value-equivalent after V7; assert new ownership foreign keys and uniqueness constraints.
- [x] **Step 2: Run migration test and verify RED** because the migration/schema is absent.
- [x] **Step 3: Implement forward-only V7 SQL** that creates a single bootstrap household, assigns all existing financial roots to it, and makes ownership non-null only after backfill. Do not insert a fake account or password.
- [x] **Step 4: Run migration tests on fresh and legacy fixtures** and verify both preserve financial data and forecast inputs. Confirm Flyway baseline-on-migrate behavior for the currently deployed V4 database.
- [x] **Step 5: Keep changes uncommitted** on the isolated feature branch until the user explicitly requests a commit.

## Task 4: Complete login/session endpoints and household context resolution

**Files:**
- Modify: `backend/src/main/java/com/finance/planner/security/SecurityConfiguration.java`.
- Modify/create: `backend/src/main/java/com/finance/planner/auth/AuthController.java`, `AuthService.java`, session/current-user DTOs.
- Create: `backend/src/main/java/com/finance/planner/security/HouseholdContextResolver.java` and an authenticated principal adapter.
- Create: `backend/src/test/java/com/finance/planner/auth/AuthControllerIT.java`.

**Interfaces:**
- `POST /api/auth/register` supports initial bootstrap when no user exists and invitation registration after bootstrap.
- `POST /api/auth/login` authenticates and establishes the server-side session.
- `POST /api/auth/logout` invalidates the session.
- `GET /api/auth/me` returns current user and household role, or `401`.
- `GET /api/auth/bootstrap` indicates whether initial registration is available; it reveals no user data.
- `POST /api/household/invitations` and `DELETE /api/household/invitations/{id}` are OWNER-only; response reveals raw invite token only on creation.
- `HouseholdContextResolver.requireContext()` returns the authenticated user's active household context or rejects the request.

- [x] **Step 1: Write failing controller/integration tests** for initial register, invited register, login, wrong password, generic auth failures, current-user response, logout invalidation, session fixation protection, cookie HttpOnly/SameSite behavior, CSRF enforcement and role restrictions.
- [x] **Step 2: Run tests and verify RED** for missing routes/session lifecycle.
- [x] **Step 3: Implement auth endpoints and context resolver**; use Spring Security session management and do not return password hashes or raw invite tokens except once at invite creation.
- [x] **Step 4: Run targeted integration tests and verify GREEN**, including two separate sessions representing two household members.
- [x] **Step 5: Keep changes uncommitted** on the isolated feature branch until the user explicitly requests a commit.

## Task 5: Scope every financial operation to the authenticated household

**Files:**
- Modify: `backend/src/main/java/com/finance/planner/entity/Assumption.java`, `Loan.java`, `ExpenseCategory.java`.
- Modify: `backend/src/main/java/com/finance/planner/repository/AssumptionRepository.java`, `LoanRepository.java`, `ExpenseCategoryRepository.java`.
- Modify: `backend/src/main/java/com/finance/planner/service/AssumptionService.java`, `LoanScheduleService.java`, `ExpenseScheduleService.java`.
- Modify: `backend/src/main/java/com/finance/planner/controller/AssumptionController.java`, `LoanController.java`, `ForecastController.java`.
- Modify: existing service/controller tests and `backend/src/test/java/com/finance/planner/PlannerApiIT.java`.

**Interfaces:**
- Every persisted financial service method receives `HouseholdContext` explicitly.
- Repositories expose household-scoped methods such as `findByHouseholdId...`; no global `findAll`/`findTop...` is used for request-driven financial access.
- Forecast and CSV use only loans/repayments and persisted schedules from the resolved household; request DTOs remain preview inputs, not authorization context.
- Seed/reset replaces only the caller's household data.

- [x] **Step 1: Write failing API tests** for two households proving assumptions, expense schedules, loans, forecast, CSV and reset are isolated; include forged household IDs, guessed loan IDs and cross-household repayment IDs.
- [x] **Step 2: Run tests and verify RED** by observing current global/single-household behavior violate isolation.
- [x] **Step 3: Add household ownership to aggregate roots and scope all repositories/services/controllers**; reject foreign aggregate IDs and ensure parent-child updates cannot cross households.
- [x] **Step 4: Run backend unit and integration suites** (`mvn verify -Dskip.integration.tests=true`, then `mvn verify -Dskip.unit.tests=true -Djacoco.skip=true`) and verify GREEN without reducing coverage thresholds.
- [x] **Step 5: Keep changes uncommitted** on the isolated feature branch until the user explicitly requests a commit.

## Task 6: Build the Vue login, registration and household invitation experience

**Files:**
- Create: `frontend/src/services/auth.js`.
- Create: `frontend/src/components/AuthView.vue`.
- Create: `frontend/src/components/HouseholdSettings.vue`.
- Modify: `frontend/src/services/api.js`, `frontend/src/App.vue`, `frontend/src/assets/style.css`.
- Create/update: frontend auth service/component/App lifecycle tests following existing Vitest conventions.

**Interfaces:**
- Auth service exports `getAuthState()`, `registerInitial(payload)`, `registerWithInvitation(payload, token)`, `login(payload)`, `logout()`, `createInvitation()`, and `revokeInvitation(id)`.
- Existing API functions send cookies on same-origin requests and attach the CSRF token for unsafe methods.
- App shell renders AuthView until `/api/auth/me` succeeds; after login it renders the existing planner unchanged and shows logout/account role. Owner sees invitation controls.

- [ ] **Step 1: Write failing frontend tests** for initial bootstrap vs invite-only registration, login/logout, session restoration, `401` redirect/state, CSRF header inclusion, owner-only invitation UI, and preservation of existing planner lifecycle after authenticated state.
- [ ] **Step 2: Run targeted Vitest tests and verify RED** for missing auth UI/service.
- [ ] **Step 3: Implement auth API service and Vue views/shell** with accessible validation/error states, one-time invite display/copy, and no raw token persistence in browser storage.
- [ ] **Step 4: Run `npm test` and `npm run build`**; verify all existing financial component tests remain green and coverage gates pass.
- [ ] **Step 5: Keep changes uncommitted** on the isolated feature branch until the user explicitly requests a commit.

## Task 7: Add Playwright end-to-end journeys for authentication and tenancy

**Files:**
- Modify: `e2e/fixtures/test.js`, `e2e/scripts/run-e2e.mjs`, `docker-compose.e2e.yml` only if necessary for auth-aware readiness/isolated database setup.
- Create: `e2e/pages/AuthPage.js`, `e2e/pages/HouseholdSettingsPage.js`.
- Create: `e2e/tests/authentication.spec.js`, `e2e/tests/household-invitations.spec.js`, `e2e/tests/household-isolation.spec.js`.
- Modify: `.github/workflows/ci.yml` only if E2E needs new environment or artifact handling.

**Interfaces:**
- Playwright fixtures create isolated household/user identities per scenario; no test depends on a pre-existing live user or production database.
- E2E stack continues using the dedicated `personal-finance-planner-e2e` Compose project and its own persisted synthetic database volume.
- Browser tests run inside the version-pinned Playwright Docker image in CI.

- [ ] **Step 1: Write E2E scenarios** for initial registration, owner creates invite, member joins, both accounts observe shared edits, logout/login, owner-only invitation restrictions and a second household's data isolation.
- [ ] **Step 2: Run targeted Playwright specs and verify RED** against the unauthenticated UI/API.
- [ ] **Step 3: Implement page objects/fixtures and adjust E2E orchestration** so each test creates isolated synthetic users/households without relying on the existing seed endpoint unauthenticated.
- [ ] **Step 4: Run `npm ci` and `npm run e2e` from `e2e/`**, using `PLAYWRIGHT_DOCKER_IMAGE=mcr.microsoft.com/playwright:v1.65.1-noble` where the host should not install browsers; verify full report and failure artifacts.
- [ ] **Step 5: Keep changes uncommitted** on the isolated feature branch until the user explicitly requests a commit.

## Task 8: Update specifications, architecture and operational documentation

**Files:**
- Modify: `specs/personal-finance-planner/spec.md`.
- Modify: relevant files linked from `docs/README.md` for architecture, API contract, business rules and data model.
- Modify: `docs/06-development-guide.md`, `docs/07-ai-agent-context.md`, and `docs/08-engineering-constitution.md` only where current statements become stale.
- Modify: editable architecture/network diagram source files and rendered docs when the repository has an existing source for them.

- [ ] **Step 1: Write documentation assertions/checks** for removal of the old single-user/no-sign-in statements and presence of bootstrap, invite, session, CSRF, household ownership, migration backup and recovery guidance.
- [ ] **Step 2: Run the documentation checks and verify RED** against stale docs.
- [ ] **Step 3: Update product/API/data/architecture docs and diagram sources** to describe actual implemented behavior, distinguish local HTTP/TLS assumptions, explain first-user bootstrap and safe upgrade from the existing V4 database.
- [ ] **Step 4: Run documentation checks, compose config validation and repository verification**; review docs against executable behavior.
- [ ] **Step 5: Keep changes uncommitted** on the isolated feature branch until the user explicitly requests a commit.

## Task 9: Full verification and security review

**Files:**
- Modify only files identified by failing tests/review findings.
- Review: all auth, household, financial ownership, frontend and E2E files created above.

- [ ] **Step 1: Run full repository verification** with `./scripts/verify.ps1` and capture actual test counts, coverage, PIT and Compose validation results.
- [ ] **Step 2: Run the full Playwright suite** with the official Docker image and inspect traces/screenshots for failures.
- [ ] **Step 3: Run local Compose upgrade verification only after a PostgreSQL backup is confirmed**; preserve `finance_planner_db_data`, do not run `down -v`, and do not reset or seed the real household data. Validate Flyway V7 and compare pre/post financial snapshots and forecast totals.
- [ ] **Step 4: Perform security review** for session fixation, CSRF, cookie attributes, login throttling, password hashing, invitation entropy/hash/expiry/replay, first-user race, IDOR, owner/member authorization, CORS and logs.
- [ ] **Step 5: Keep all final fixes uncommitted** until the user explicitly requests a commit; report the actual Git status and do not push/deploy unless separately requested.

## Execution notes

- Follow the Superpowers TDD iron law: no production behavior code before a test has failed for the expected reason. Configuration/dependency edits are introduced with the task's failing tests.
- Do not implement all layers at once. Complete each task's red/green cycle and preserve a runnable application at each meaningful boundary.
- The design document's builder-check was self-reviewed because the Laptop subagent worker run was not addressable from this follow-up request. The design leaves password policy, identifier normalization, session duration, invitation expiry and exact endpoint naming to the builder; choose conservative defaults and document them.
- Before any operation against the user's live local database, verify a fresh backup exists and obtain explicit authorization for that state-changing migration/deployment. Test migrations against isolated synthetic fixtures first.
