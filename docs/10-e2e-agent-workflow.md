# 10. E2E Agent Workflow

## Purpose

This document defines how Chat on Steroids coordinates the Playwright Planner, Generator and Healer workflows for Personal Finance Planner. It is a routing convention, not a claim that the three Codex TOML agents are automatically callable from Chat on Steroids.

## Specialist responsibilities

| Specialist | Use when | Owns | Does not own |
|---|---|---|---|
| Planner | Discover journeys, assess coverage, design scenarios | UI exploration and test plans in `e2e/plans/` | Implementing specs or changing business rules |
| Generator | Implement an approved plan | Playwright specs in `e2e/tests/` and Page Objects in `e2e/pages/` | Inventing requirements or financial calculation oracles |
| Healer | Diagnose observed E2E failures | Root-cause analysis and minimal test-only repairs | Hiding application defects or changing product behavior |

## Routing policy

```text
User request
    |
    v
QA Orchestrator
    |
    +-- coverage / exploration / test plan ------> Planner
    |
    +-- approved plan + request to implement ----> Generator
    |                                                |
    |                                                v
    |                                         focused E2E run
    |                                                |
    +-- failing E2E / flaky test ----------------> Healer
    |                                                |
    +<---------------- evidence and report <---------+
```

For a broad “add E2E for feature X” request, run Planner first, review the saved plan against the accepted SpecKit specification, then hand off to Generator because the user requested implementation. For a plan-only request, stop after Planner. Do not invoke every specialist for every request.

## How Chat on Steroids invokes work

- The project Skill `personal-finance-planner-qa-orchestrator` performs intent routing and applies the constraints in this document.
- Specialist Skills under `/skills/playwright-test-{planner,generator,healer}/SKILL.md` provide the Chat on Steroids-specific operating instructions.
- Chat on Steroids worker delegation (the `agents` capability) creates separate ChatGPT worker chats. It is distinct from Codex subagents.
- `.codex/agents/playwright_test_{planner,generator,healer}.toml` are canonical Codex agent definitions for Codex-compatible clients. Their presence does not prove Chat on Steroids has launched them.

## Non-negotiable safety and quality constraints

1. Browser testing targets only `http://127.0.0.1:18088`, the isolated E2E stack.
2. Use synthetic data only. Never inspect or mutate the normal household database or its volume.
3. Playwright remains isolated under `e2e/`; plans under `e2e/plans/`; specs under `e2e/tests/`; Page Objects under `e2e/pages/`.
4. Keep execution sequential while scenarios share the E2E database.
5. Backend JUnit tests remain authoritative for financial arithmetic. Do not add a JavaScript forecast oracle.
6. Healer must not weaken assertions, change expected financial values, add arbitrary sleeps, skip/fixme tests, or silently edit application/business/seed/Compose/CI behavior.
7. Preserve unrelated working-tree changes. No commit or push without an explicit request.

## Handoff contract

Every specialist handoff must include the user goal, relevant accepted requirement IDs, exact input artifact paths, constraints, expected output paths and verification requested. Planner must provide a reproducible synthetic starting state. Generator must consume an approved plan. Healer must receive observed failure evidence (command, output, trace or screenshot where available).

## Reporting

The orchestrator reports the selected specialist(s), changed paths, requirement traceability, exact commands and observed test counts, whether the isolated stack was used, unresolved failures, and current Git state. “Not run” must never be reported as “passed.”

## Related configuration

- Orchestrator Skill: `/skills/personal-finance-planner-qa-orchestrator/SKILL.md`
- Planner Skill: `/skills/playwright-test-planner/SKILL.md`
- Generator Skill: `/skills/playwright-test-generator/SKILL.md`
- Healer Skill: `/skills/playwright-test-healer/SKILL.md`
- Codex agent definitions: `.codex/agents/`
- E2E package: `e2e/`
