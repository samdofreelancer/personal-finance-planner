# Effective-Month Income Schedule Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace annual percentage raises with a persisted schedule of explicit monthly income amounts effective from user-selected months.

**Architecture:** Normalize income changes in `income_amount_changes`, expose them through `AssumptionDTO.incomeChanges`, and resolve the effective amount in the backend forecast service. Reuse the existing assumptions save/preview/export snapshot contract and the expense schedule editing pattern.

**Tech Stack:** Java 21, Spring Boot/JPA, Flyway, PostgreSQL/H2, Vue 3, Vitest, Playwright.

**Spec:** `specs/005-income-schedule/spec.md`; design: `docs/superpowers/specs/2026-11-21-effective-month-income-schedule-design.md`.

## Global Constraints

- Forecast starts in November 2026 and supports horizons 1–6 years.
- Money is non-negative whole VND and backend validation is authoritative.
- Forecast and CSV preview do not persist assumptions; explicit Save does.
- Keep business calculations in Java; do not create a JavaScript forecast oracle.
- Preserve existing uncommitted horizon, auth, expense and other work; do not commit or push.
- Do not delete or reset any database volume or real household data.

## Review Focus

- Missing November 2026 baseline: backend rejects the payload before persistence.
- Duplicate/out-of-order entries: lookup is order-independent and duplicates are rejected.
- Future entry beyond selected horizon: retained on save but has no effect on current forecast months.
- January multiplier after a scheduled change: applies to the selected effective amount for that January.
- Legacy migration rounding: each converted monthly amount is rounded HALF_UP to whole VND and unrelated data remains unchanged.

---

### Task 1: Backend income schedule domain and validation

**Files:**
- Create: `backend/src/main/java/com/finance/planner/dto/IncomeChangeDTO.java`
- Create: `backend/src/main/java/com/finance/planner/service/IncomeScheduleService.java`
- Modify: `backend/src/main/java/com/finance/planner/dto/AssumptionDTO.java`
- Modify: `backend/src/main/java/com/finance/planner/service/ValidationService.java`
- Test: `backend/src/test/java/com/finance/planner/service/ValidationServiceTest.java`

**Interfaces:**
- `IncomeChangeDTO`: `String effectiveMonth`, `Long amount`.
- `AssumptionDTO`: `List<IncomeChangeDTO> incomeChanges` replaces `baseSalary` and `raises`.
- `IncomeScheduleService.amountAt(List<IncomeChangeDTO> changes, YearMonth period): long` returns the latest amount effective at or before the period.
- `ValidationService.validate(AssumptionDTO)` validates exactly one `2026-11` baseline, valid months >= `2026-11`, unique months and non-negative whole VND amounts.

- [x] **Step 1: Write failing validation and lookup tests**
  - `acceptsIncomeScheduleWithBaselineAndFutureChanges`
  - `rejectsMissingIncomeSchedule`
  - `rejectsMissingNovember2026Baseline`
  - `rejectsDuplicateOrPreBaselineEffectiveMonths`
  - `rejectsNegativeOrMissingIncomeAmounts`
  - `usesLatestEffectiveIncomeChangeRegardlessOfInputOrder`
  - `usesPreviousIncomeAfterFutureChangeIsRemoved`
- [x] **Step 2: Run focused tests and confirm expected failures**
  - Run `mvn -Dtest=ValidationServiceTest,IncomeScheduleServiceTest test` from `backend/`; initial compile/test failures confirmed the new contract was not implemented.
- [x] **Step 3: Implement DTO, schedule lookup and validation**
- [x] **Step 4: Run focused tests and confirm pass**

### Task 2: Forecast calculation uses effective income

**Files:**
- Modify: `backend/src/main/java/com/finance/planner/service/ForecastService.java`
- Test: `backend/src/test/java/com/finance/planner/service/ForecastServiceTest.java`

**Interfaces:**
- `ForecastService` consumes `AssumptionDTO.incomeChanges` and `IncomeScheduleService.amountAt`.
- Monthly salary is the effective scheduled amount, multiplied by `januaryMultiplier` only in January, then rounded HALF_UP to whole VND.

- [x] **Step 1: Write failing forecast tests**
  - `appliesIncomeChangeStartingInItsEffectiveMonth`
  - `supportsSixYearHorizonAndAppliesScheduledIncomeFromItsEffectiveMonth`
  - `appliesJanuaryMultiplierToEffectiveIncomeAmount`
  - `keepsFutureIncomeChangesOutsideHorizonWithoutChangingCurrentMonths`
- [x] **Step 2: Run focused forecast tests and confirm expected failures**
  - Run `mvn -Dtest=ForecastServiceTest test` from `backend/`.
- [x] **Step 3: Implement effective-month forecast lookup**
- [x] **Step 4: Run focused forecast tests and confirm pass**

### Task 3: Persist schedule and migrate existing data

**Files:**
- Create: `backend/src/main/java/com/finance/planner/entity/IncomeAmountChange.java`
- Create: `backend/src/main/java/com/finance/planner/repository/IncomeAmountChangeRepository.java`
- Create: `backend/src/main/java/com/finance/planner/service/IncomeSchedulePersistenceService.java` (or fold persistence into `IncomeScheduleService` if it remains cohesive)
- Create: `backend/src/main/resources/db/migration/V6__replace_salary_raises_with_income_schedule.sql`
- Modify: `backend/src/main/java/com/finance/planner/service/AssumptionService.java`
- Modify: `backend/src/main/java/com/finance/planner/entity/Assumption.java`
- Test: `backend/src/test/java/com/finance/planner/integration/IncomeScheduleMigrationIT.java`
- Test: `backend/src/test/java/com/finance/planner/integration/PlannerApiIT.java`

**Interfaces:**
- `GET/PUT /api/assumptions` read and atomically replace the complete income schedule with the rest of the assumptions.
- V6 creates `income_amount_changes(assumption_id, effective_month, amount)` with FK cascade, unique `(assumption_id, effective_month)` and non-negative amount check.
- Migration inserts `2026-11` base salary plus April entries for 2027–2032 using compounded prior raises and whole-VND HALF_UP rounding, then drops `salary_raise_rates` and `assumptions.base_salary`.

- [x] **Step 1: Write failing migration and API persistence tests**
  - `migratesBaseSalaryAndCompoundedRaiseRatesToEffectiveMonthAmounts`
  - `savesAndReloadsCompleteIncomeSchedule`
  - `rejectsInvalidIncomeScheduleWithoutChangingSavedAssumptions`
- [x] **Step 2: Run focused integration tests and confirm expected failures**
  - Run `mvn -Dit.test=IncomeScheduleMigrationIT,PlannerApiIT verify` from `backend/`.
- [x] **Step 3: Implement V6 and schedule persistence**
- [x] **Step 4: Run migration and API integration tests and confirm pass**

### Task 4: Frontend income schedule editor

**Files:**
- Modify: `frontend/src/components/AssumptionsForm.vue`
- Modify: `frontend/src/utils/validation.js`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/services/api.js` only if request serialization requires it
- Test: `frontend/test/components.test.js`
- Test: `frontend/test/validation.test.js`
- Test: `frontend/test/app.test.js`

**Interfaces:**
- Assumptions payload uses `incomeChanges: [{ effectiveMonth, amount }]`; remove `baseSalary` and `raises` from UI state and request payload.
- The editor always renders a non-removable `2026-11` baseline and supports adding/removing future rows; all rows support editing month and amount.

- [x] **Step 1: Write failing component and validation tests**
  - `renders and emits an editable effective-month income schedule`
  - `adds and removes future income changes but keeps the required baseline`
  - `allows correcting or removing a future entry that temporarily duplicates the baseline month`
  - `preserves income changes beyond the selected forecast horizon`
  - `rejects duplicate, invalid and pre-baseline income months`
- [x] **Step 2: Run focused frontend tests and confirm expected failures**
  - Run `npx vitest run test/components.test.js test/validation.test.js test/app.test.js` from `frontend/`.
- [x] **Step 3: Implement schedule editor and payload validation**
- [x] **Step 4: Run focused frontend tests and confirm pass**

### Task 5: End-to-end coverage, documentation and full verification

**Files:**
- Modify: `e2e/tests/assumptions.spec.js` or create `e2e/tests/income-schedule.spec.js` following existing Page Object patterns
- Modify: `docs/03-business-rules.md`
- Modify: `docs/04-api-contract.md`
- Modify: `docs/05-data-model.md`
- Modify: `docs/06-development-guide.md`
- Modify: `docs/07-ai-agent-context.md`
- Modify: `specs/personal-finance-planner/spec.md`
- Modify: `specs/004-forecast-horizon/spec.md` to remove percentage raise coupling

- [x] **Step 1: Write Playwright scenario**
  - Add income changes, save, reload, verify rows and monthly forecast reflect the effective month boundaries.
- [x] **Step 2: Run the Playwright scenario and verify persistence plus forecast boundaries**
  - Run `node scripts/run-e2e.mjs --grep="effective-month income changes"` from `e2e/`; 1 passed. Full suite later passed 6/6.
- [x] **Step 3: Update business/API/data-model/spec documentation**
- [x] **Step 4: Run complete verification**
  - Backend `mvn verify`: 49 unit tests and 16 integration tests passed; JaCoCo gate passed.
  - Backend PIT: 75/75 mutations killed.
  - Frontend `npm test`: 68 tests passed; 90.36% statements, 93.19% lines.
  - Frontend production build passed.
  - Docker Compose config passed. Sonar Compose config initially lacked `SONAR_DB_PASSWORD` in `.env`; separately validated successfully using a temporary validation-only env file (no containers started).
  - Isolated Playwright E2E suite: 6/6 passed.
- [x] **Step 5: Review complete diff and Git status**
  - Confirm existing unrelated and pre-existing working-tree changes are preserved.
  - Do not commit or push without a separate request.
