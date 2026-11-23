# ADR-0001: Local-first modular monolith

- **Status:** Accepted baseline
- **Date:** 2026-11-13

## Context

The product currently serves one household through a local web application. Its core workload is editing one assumptions set, calculating a deterministic forecast and exporting the result. There is no approved requirement for multi-user access, cloud deployment, independent scaling or independently deployable business capabilities.

## Decision

- Keep the application as a modular monolith with one Spring Boot backend deployment.
- Keep Vue as a separate presentation tier served by Nginx.
- Use PostgreSQL as the persistence store for the household assumptions.
- Keep forecast calculation in the backend; frontend is responsible for interaction and presentation, not an authoritative calculation implementation.
- Keep deployment local-first and minimize network exposure by default.

## Consequences

### Positive

- Lower operational complexity than microservices.
- One authoritative backend calculation path can serve UI, tests and CSV export.
- Clear boundaries can still exist inside the monolith (API, application services, forecast policies, persistence).

### Trade-offs

- Backend modules are released together.
- A single assumptions record does not support multiple households or scenarios.
- If product requirements later change, identity/ownership and scenario behavior require explicit domain design rather than an incidental database column.

## Revisit when

- An approved requirement introduces multiple users/households or scenario management.
- Independent scaling/deployment becomes a demonstrated need.
- The local-first deployment constraint is intentionally changed.
