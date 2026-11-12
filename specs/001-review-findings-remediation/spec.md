# Feature Specification: System Review Findings Remediation

**Feature Branch**: `001-review-findings-remediation`  
**Created**: 2026-11-12  
**Status**: Draft — review baseline captured  
**Input**: User-approved system design review findings for Personal Finance Planner

## 1. Purpose

Track the findings from the system design review as actionable, testable requirements before implementation. Improve safety, financial correctness, reliability, and maintainability without expanding into unapproved product features.

This specification is the remediation backlog's source of truth. Preserve finding identifiers `F-01` through `F-10` for traceability.

## 2. Scope and prioritization

| Finding | Priority | Area | Remediation outcome |
|---|---|---|---|
| F-01 | P1 | Security / deployment | Enforce local-only exposure; restrict CORS and published ports |
| F-02 | P1 | Persistence reliability | Prevent older autosave requests overwriting newer assumptions |
| F-03 | P1 | Financial correctness | Use deterministic decimal-safe monetary calculations |
| F-04 | P1 | Testability / source of truth | Test backend forecast behavior; retire duplicate production calculation paths |
| F-05 | P2 | Maintainability | Give forecast calculation responsibilities explicit, testable boundaries |
| F-07 | P2 | Database evolution | Version and reproduce schema changes through migrations |
| F-09 | P2 | API contract | Separate saved forecast from temporary non-persisting simulation |
| F-10 | P2 | Delivery traceability | Keep findings, requirements, tasks, and verification evidence synchronized |

### Deferred findings

- **F-06 — Scenario domain and scenario management:** future product capability. Do not add scenario CRUD, comparison UI, or scenario persistence in this remediation. Revisit when the product requirement is approved.
- **F-08 — Multiple assumptions ownership/identity:** coupled to the deferred scenario capability. Do not introduce household/user tenancy or multi-profile persistence in this remediation. Preserve the single-household local-app behavior.

## 3. Users and operating context

- One household uses the application in a local deployment.
- Assumptions and forecast results are sensitive household data.
- Incorrect arithmetic or stale persisted assumptions can lead to materially misleading decisions.
- Existing forecast rules and displayed results remain authoritative unless a behavior change is explicitly approved.

## 4. User Scenarios & Testing

### User Story 1 — Keep private financial data local (Priority: P1)

As the household user, I want the application reachable only through the intended local machine by default, so financial assumptions are not unintentionally exposed to the local network.

**Why this priority**: The application contains sensitive financial data and currently has permissive network configuration.

**Independent Test**: Inspect effective Docker port bindings and CORS behavior; verify localhost works and non-local interfaces/origins are not granted access by default.

**Acceptance Scenarios**:

1. **Given** the default local deployment, **When** containers start, **Then** frontend/backend ports bind to loopback only.
2. **Given** the default local deployment, **When** the application starts, **Then** PostgreSQL is not published to the host unless explicitly required.
3. **Given** a browser request from an unapproved origin, **When** it calls the backend, **Then** the backend does not grant cross-origin access.
4. **Given** intentional network exposure is required, **When** an operator configures it, **Then** allowed origins and exposure are explicit and documented.

### User Story 2 — Persist the latest assumptions reliably (Priority: P1)

As the household user, I want autosave to preserve the newest values I entered, so a slower earlier request cannot overwrite a later edit.

**Why this priority**: Stale persisted assumptions can silently make later forecasts differ from what the UI showed.

**Independent Test**: Simulate overlapping saves where the older request completes last; verify persisted state remains the newest submitted state.

**Acceptance Scenarios**:

1. **Given** save A contains older assumptions and save B contains newer assumptions, **When** A completes after B, **Then** persisted assumptions remain those from B.
2. **Given** a save is in progress, **When** the user makes additional edits, **Then** writes are serialized or protected by a server-enforced concurrency strategy.
3. **Given** a save fails, **When** the UI reports the failure, **Then** the user can distinguish unsaved changes from successfully persisted assumptions.
4. **Given** the user reloads after a successful save, **When** assumptions are loaded, **Then** values equal the latest successfully saved state.

### User Story 3 — Produce financially deterministic forecasts (Priority: P1)

As the household user, I want monetary calculations to follow explicit decimal and rounding rules, so results do not depend on binary floating-point artifacts or inconsistent rounding.

**Why this priority**: Arithmetic correctness is a core product requirement for household financial planning.

**Independent Test**: Test boundary values, fractional percentage calculations, rounding boundaries, and approved forecast examples.

**Acceptance Scenarios**:

1. **Given** supported monetary and rate inputs, **When** the forecast is calculated, **Then** monetary arithmetic uses decimal-safe operations end-to-end.
2. **Given** a calculation produces a fractional VND amount, **When** it is converted to whole VND, **Then** the documented rounding mode is applied consistently.
3. **Given** inputs exceed supported bounds or risk overflow, **When** they are submitted, **Then** the API rejects them instead of returning a misleading forecast.
4. **Given** baseline assumptions, **When** the forecast is calculated, **Then** approved business rules and expected outputs remain unchanged except for explicitly approved rounding corrections.

### User Story 4 — Trust backend forecast behavior and tests (Priority: P1)

As a maintainer, I want the backend forecast engine to be the authoritative production calculation path and to have automated tests, so test success provides evidence about the code that serves users.

**Why this priority**: Existing JavaScript tests do not verify the Java backend implementation.

**Independent Test**: Run backend unit tests for forecast rules against approved fixtures; verify no production endpoint uses an independent duplicate calculation implementation.

**Acceptance Scenarios**:

1. **Given** the backend forecast implementation, **When** its test suite runs, **Then** it executes meaningful tests rather than reporting no tests found.
2. **Given** baseline fixtures, **When** the backend calculates them, **Then** results match approved expected values and rounding rules.
3. **Given** a defect is introduced into a backend calculation rule, **When** the relevant test runs, **Then** the test fails.
4. **Given** migration from the legacy JavaScript calculation is complete, **When** production forecast requests are served, **Then** Java is the only production source of truth; any retained JavaScript oracle is test-only and documented.

### User Story 5 — Evolve the forecast engine safely (Priority: P2)

As a maintainer, I want calculation responsibilities separated along clear domain boundaries, so individual financial policies can be understood and tested without changing unrelated behavior.

**Why this priority**: The central forecast service currently combines multiple responsibilities.

**Independent Test**: Verify extracted policies have focused unit tests and the composed forecast preserves approved end-to-end results.

**Acceptance Scenarios**:

1. **Given** forecast calculation, **When** responsibilities are organized, **Then** income, expenses, debt payoff, savings accumulation, and orchestration have explicit boundaries appropriate to their behavior.
2. **Given** an individual policy, **When** it is tested, **Then** it can be tested without starting the web server or database.
3. **Given** the refactored engine, **When** a full forecast is calculated, **Then** response contract and approved baseline outputs remain compatible.

### User Story 6 — Apply reproducible database schema changes (Priority: P2)

As a maintainer, I want schema changes versioned and repeatable, so fresh and existing installations reach the same schema predictably.

**Why this priority**: Implicit Hibernate schema updates are difficult to review and reproduce.

**Independent Test**: Initialize an empty database and upgrade a database at the current schema; verify migrations apply in order and Hibernate does not mutate schema automatically.

**Acceptance Scenarios**:

1. **Given** an empty PostgreSQL database, **When** the backend starts, **Then** versioned migrations create the required schema.
2. **Given** a database at an earlier supported version, **When** the backend starts, **Then** pending migrations apply in order without losing saved assumptions.
3. **Given** a migration-managed schema, **When** Hibernate starts, **Then** automatic schema mutation is disabled; schema validation may be used.
4. **Given** a migration fails, **When** startup is attempted, **Then** failure is visible and the application does not silently continue with a partially upgraded schema.

### User Story 7 — Distinguish saved forecast from temporary simulation (Priority: P2)

As the household user, I want a forecast based on saved assumptions and a separate temporary simulation, so I can explore what-if decisions without changing my persisted plan.

**Why this priority**: The current forecast endpoint accepts query parameters while assumptions are also persisted separately, leaving the source of forecast inputs ambiguous.

**Independent Test**: Save assumptions A, simulate assumptions B, then reload saved assumptions and forecast; verify simulation did not mutate A.

**Acceptance Scenarios**:

1. **Given** saved assumptions A, **When** the user requests the normal forecast, **Then** the backend calculates from A loaded from persistence.
2. **Given** saved assumptions A, **When** temporary assumptions B are submitted to simulation, **Then** the response is calculated from B and no persistence write occurs.
3. **Given** simulation B has completed, **When** saved assumptions or normal forecast are requested, **Then** the result still reflects A.
4. **Given** an invalid simulation request, **When** the backend processes it, **Then** it returns validation errors and leaves saved assumptions unchanged.

### User Story 8 — Keep review-to-delivery traceability (Priority: P2)

As a product owner and maintainer, I want each review finding mapped to requirements, implementation tasks, and verification evidence, so no finding is silently dropped or closed without proof.

**Why this priority**: Existing project planning documents still contain initial scaffolding tasks and do not accurately represent completed implementation.

**Independent Test**: Audit the remediation tracker and confirm each in-scope finding has linked requirements, tasks, and evidence or an explicit deferral.

**Acceptance Scenarios**:

1. **Given** an in-scope finding, **When** implementation planning is prepared, **Then** it maps to requirement IDs and implementation tasks.
2. **Given** a task is marked complete, **When** its status is reviewed, **Then** test, build, or inspection evidence is recorded.
3. **Given** a finding is deferred, **When** the backlog is reviewed, **Then** its rationale and revisit condition are documented.
4. **Given** existing project-level spec/plan/tasks documents, **When** remediation planning is approved, **Then** stale status/boilerplate is corrected or explicitly marked historical without silently changing original product scope.

## 5. Functional Requirements

### F-01 — Security and local deployment

- **FR-001**: Default deployment MUST bind published application ports to loopback interfaces only.
- **FR-002**: Default deployment MUST NOT publish the PostgreSQL port to the host.
- **FR-003**: Backend MUST use an explicit allowlist of frontend origins; wildcard origin access MUST NOT be the default.
- **FR-004**: Deployment documentation MUST explain intentional exposure beyond localhost and its security implications.
- **FR-005**: Database credentials MUST be configurable outside committed source-controlled defaults; sample configuration MUST NOT encourage weak credential reuse.

### F-02 — Autosave consistency

- **FR-006**: System MUST prevent an older assumptions update from overwriting a newer update.
- **FR-007**: Persistence MUST provide a defined ordering/concurrency guarantee, such as serialized client writes or server-side optimistic concurrency control.
- **FR-008**: Frontend MUST represent save success/failure accurately and MUST NOT present an unpersisted value as confirmed saved state.

### F-03 — Monetary correctness

- **FR-009**: Backend monetary calculations MUST use decimal-safe arithmetic end-to-end; binary floating-point types MUST NOT be used for monetary values or intermediate monetary calculations.
- **FR-010**: System MUST define and consistently apply a rounding policy when converting calculated amounts to whole VND.
- **FR-011**: API MUST validate monetary amounts, rates, and derived values against documented supported bounds before calculation or persistence.
- **FR-012**: Backend MUST preserve approved forecast business rules and expose intentional output changes through reviewed tests and documentation.

### F-04 — Backend test coverage and single calculation source

- **FR-013**: Backend MUST have automated unit tests for forecast calculation rules.
- **FR-014**: Backend tests MUST cover baseline values, debt payoff, provisional interest, partial-year aggregation, negative savings/shortfall, percentage raises, bonus multiplier, and rounding/boundary behavior applicable to current rules.
- **FR-015**: Java backend MUST be the authoritative production forecast calculation path.
- **FR-016**: Retained legacy JavaScript calculation MUST be test-only, clearly identified, and MUST NOT independently serve production forecast behavior.

### F-05 — Forecast engine boundaries

- **FR-017**: Forecast calculation MUST separate policy responsibilities sufficiently for focused tests without web/database infrastructure.
- **FR-018**: Refactoring MUST preserve the public forecast response contract unless a separately reviewed contract change is approved.
- **FR-019**: Design MUST avoid unnecessary microservices or persistence entities created solely to mirror code-level classes.

### F-07 — Database migrations

- **FR-020**: Schema evolution MUST be managed by ordered, version-controlled migrations.
- **FR-021**: Automatic Hibernate schema mutation MUST be disabled in normal runtime.
- **FR-022**: Migrations MUST support fresh database initialization and upgrade of the currently supported schema without losing saved assumptions.

### F-09 — Forecast API semantics

- **FR-023**: Normal forecast operation MUST calculate using currently persisted assumptions.
- **FR-024**: Separate simulation operation MUST accept temporary assumptions and MUST NOT persist them.
- **FR-025**: API documentation MUST make the input source and persistence side effects explicit for each operation.
- **FR-026**: Validation and calculation behavior MUST be consistent between normal forecast and simulation operations.

### F-10 — Traceability

- **FR-027**: Each remediation task MUST reference the finding ID(s) and requirement ID(s) it addresses.
- **FR-028**: Each completed remediation task MUST include verification evidence.
- **FR-029**: Deferred findings MUST remain visible with rationale and a revisit trigger.
- **FR-030**: This remediation specification MUST be the source of truth for review-finding scope; original product scope MUST NOT be silently broadened.

## 6. Non-Functional Requirements

- **NFR-001**: Forecast calculation MUST be deterministic for identical inputs and the same supported calculation version.
- **NFR-002**: Core forecast tests MUST run without PostgreSQL or a running HTTP server unless testing persistence/API integration.
- **NFR-003**: Default local deployment MUST minimize network exposure.
- **NFR-004**: Invalid inputs or failed persistence MUST produce actionable errors and MUST NOT silently fall back to stale assumptions.
- **NFR-005**: Remediation MUST preserve the single-household, local-first operating model.

## 7. Success Criteria

- **SC-001**: Default deployment exposes frontend/backend only on loopback and does not publish PostgreSQL.
- **SC-002**: An automated concurrency test proves an older save cannot overwrite newer assumptions.
- **SC-003**: Backend forecast tests execute meaningful cases and cover all current calculation rules listed in FR-014.
- **SC-004**: No backend monetary calculation path uses `double` or `float`.
- **SC-005**: Approved baseline fixtures pass under the documented decimal/rounding policy.
- **SC-006**: Fresh database initialization and upgrade from the current schema both succeed using migrations.
- **SC-007**: Normal forecast reads persisted assumptions; simulation uses request assumptions without changing persisted state.
- **SC-008**: Every in-scope finding has traceable requirements and verification evidence before closure.

## 8. Assumptions and Constraints

- Application remains a single-household local application; authentication, multi-tenancy, cloud hosting, and multi-user access are not introduced here.
- Existing forecast rules remain authoritative unless a discrepancy is discovered and explicitly resolved.
- Financial values remain whole VND at API/persistence boundaries unless existing contracts require otherwise; intermediate calculations use decimal-safe precision.
- Exact autosave concurrency mechanism is an implementation decision if observable consistency requirements are met.
- Exact Forecast Engine class decomposition is an implementation decision; avoid unnecessary abstraction.
- Exact simulation API path is an implementation decision; its contract must distinguish persisted forecast from non-persisting simulation.

## 9. Out of Scope

- Scenario creation, editing, comparison, versioning, and scenario-specific persistence (deferred F-06).
- Multiple households, user accounts, tenant ownership, or assumptions ownership identity (deferred F-08).
- Investments, tax, inflation, bank synchronization, or financial advice.
- Microservice extraction.
- UI redesign unrelated to save-state feedback or the simulation interaction required by F-09.

## 10. Finding Register

| Finding ID | Review observation | Disposition | Linked requirements |
|---|---|---|---|
| F-01 | Permissive CORS and host port exposure can expose sensitive local financial data | In scope — P1 | FR-001–FR-005 |
| F-02 | Overlapping autosave PUT requests can persist stale assumptions | In scope — P1 | FR-006–FR-008 |
| F-03 | Binary floating-point arithmetic risks monetary precision/rounding errors | In scope — P1 | FR-009–FR-012 |
| F-04 | Legacy JavaScript and Java duplicate forecast logic; existing tests do not verify backend | In scope — P1 | FR-013–FR-016 |
| F-05 | ForecastService combines multiple responsibilities | In scope — P2 | FR-017–FR-019 |
| F-06 | No Scenario domain/model exists | Deferred — future product feature | Revisit when scenario management is approved |
| F-07 | Hibernate ddl-auto update is used instead of explicit migrations | In scope — P2 | FR-020–FR-022 |
| F-08 | Single assumptions row has no scenario/owner identity | Deferred — depends on F-06/product scope | Revisit with scenario/ownership requirements |
| F-09 | Forecast endpoint input source is ambiguous between persisted assumptions and query parameters | In scope — P2 | FR-023–FR-026 |
| F-10 | SpecKit tasks/status do not accurately reflect implementation completion | In scope — P2 | FR-027–FR-030 |

## 11. Risks and Open Decisions

- **R-01 — Rounding compatibility:** Decimal-safe arithmetic may expose existing rounding differences. Baseline fixtures and an explicit rounding decision are required before implementation is complete.
- **R-02 — Autosave strategy:** Serialized writes are simpler for a single-user local app; optimistic locking may be appropriate if concurrent clients become supported. Choose the simplest strategy that proves the guarantee.
- **R-03 — API compatibility:** Making normal forecast read persisted assumptions may affect frontend request flow. Preserve a clearly separated simulation contract and update consumers together.
- **R-04 — Existing database migration:** Test migration against a copy/backup of the current schema before relying on it for user data.
- **R-05 — Finding F-05 decomposition:** Do not split classes mechanically. Extract cohesive policies with meaningful tests.
