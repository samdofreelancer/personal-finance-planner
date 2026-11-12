# 07. AI Agent Context

## Why this document exists

This file gives coding agents a compact, reliable mental model of the repository. It is a navigation aid, not a replacement for source code, tests, specifications or detailed documentation.

## Product identity

This repository contains **Personal Finance Planner** (UI branding: Planwise): a local-first planning tool for one household. It lets the household edit financial assumptions, inspect a month-by-month forecast, compare annual summaries and export forecast data as CSV.

Do not silently broaden this product into a bank aggregator, accounting system, investment advisor, multi-tenant SaaS or the separate Financial GPS domain model. Those would require explicit product decisions and specifications.

## Current system at a glance

- Frontend: Vue 3 + Vite; production assets are served by Nginx.
- Backend: Spring Boot 3.3.5, Java 21, REST API and Spring Data JPA.
- Database: PostgreSQL 16; current persistence is a single logical `assumptions` record.
- Local orchestration: Docker Compose (`frontend`, `backend`, `db`).
- Forecast horizon: November 2026 through December 2030 (50 months).
- Backend Java is the single implementation of forecast calculations.
- No authentication, household tenancy, bank integration or cloud deployment is implemented.

## Source-of-truth hierarchy

When sources disagree, use this order and report the discrepancy:

1. Current executable source and runtime configuration for **as-is behavior**.
2. Automated tests and observed verification results for behavior they actually cover.
3. Accepted SpecKit specification for **intended scope and acceptance criteria**.
4. Architecture/business/API/data docs for the documented model; update them when stale.
5. README prose, comments and old planning notes.

Never convert a planned feature or open review finding into a claim that it is implemented. Label statements as `as-is`, `specified/to-be`, `decision`, or `open question` where ambiguity matters.

## First-pass repository exploration

Before changing code:

1. Read this file and `docs/08-engineering-constitution.md`.
2. Read `docs/README.md`, then the relevant architecture, business rules, API and data model sections.
3. Inspect the relevant frontend/backend implementation, configuration and tests.
4. Find the matching SpecKit artifact and identify its requirement/acceptance-criteria IDs.
5. Check `git status` and preserve unrelated user changes.
6. Trace the full path affected by the change: UI → API → validation/service → persistence or forecast → response/export.

## Domain and behavior guardrails

- Forecast inputs may be supplied by the frontend as request parameters. Verify the controller and service path before assuming saved database values are used.
- Distinguish persisted assumptions from transient forecast/simulation inputs.
- Seed values are demo defaults, never the user's real financial profile.
- Money behavior must be deterministic: document currency units, precision, rounding mode, timing and boundary conditions.
- Preserve the documented loan payoff, expense-change and salary-raise timing unless an accepted requirement explicitly changes them.
- CSV exports contain household financial information; treat them as sensitive output.
- Autosave debounce and stale-response protection do not, by themselves, prove database writes are serialized.

## Preferred agent workflow

Use the repository's installed workflow tools when available, without assuming a tool ran merely because its name appears in a document:

1. **SpecKit — requirements only:** use `/speckit-specify` and `/speckit-clarify` to define scope, behavior and acceptance criteria. Do not use `/speckit-plan` or `/speckit-tasks` in this workflow.
2. **Archify — architecture/context:** after the specification is accepted, inspect the actual architecture, dependencies, boundaries and drift; update architecture artifacts when justified.
3. **Superpowers — implementation:** create the implementation task plan, follow TDD, implement incrementally, review and verify.
4. Update docs, diagram source and traceability evidence alongside the implementation.

If one of these tools is unavailable, state the limitation and follow the same intent manually; do not fabricate tool output.

## Expected final report from an agent

Every non-trivial change report should state:

- What changed and why, mapped to requirement IDs where applicable.
- Important design decisions and any deviations from the specification.
- Tests/builds/checks actually run, with pass/fail and relevant gaps.
- Docs/API/data/diagram impacts.
- Known risks, unresolved questions and work intentionally not done.
- Git state; never claim commit or push unless it occurred.

