# 006 — Always-open self-service registration

## Status

Accepted — product behavior confirmed by the user on 2026-11-22.

## Problem

The existing registration gate allowed only the first account to self-register. Every later account had to use an invitation, even when the person intended to create an independent financial plan. This incorrectly coupled account creation to household membership.

## Product decisions

1. Self-registration is always available, regardless of existing account count.
2. A registration without an invitation creates a new `User`, a new `Household`, and an `OWNER` membership in that household.
3. A registration with a valid invitation continues to create a `MEMBER` in the invitation's existing household.
4. One user belongs to one household. Independent self-registered users never share assumptions, income schedules, expense categories, loans or repayments.
5. The existing household created by migration V7 keeps the legacy financial records. New registrations must not be attached to it and must not migrate, copy, reset or overwrite its data.
6. A newly created household starts with blank financial data on first use: income baseline `2026-11 = 0`, starting savings `0`, no expense categories and no loans. Forecast years defaults to `4`; January multiplier defaults to `2.0`.
7. Loading synthetic sample data remains an explicit, household-scoped reset action; it is not part of registration or first-use initialization.

## Acceptance criteria

- **AC-01 Always-open registration:** The unauthenticated login view always exposes a control to switch to registration; the public bootstrap contract never closes registration after the first user.
- **AC-02 Independent household:** Each successful registration without an invitation returns an OWNER account associated with a newly created household ID, distinct from all existing households.
- **AC-03 Invitation compatibility:** Registration with a valid single-use invitation still joins that invitation's household as MEMBER and consumes the invitation.
- **AC-04 Blank first-use data:** The first assumptions read for a new household creates a zero income baseline and zero starting savings, with empty expenses and loans.
- **AC-05 Isolation:** Reads, writes, forecast and CSV export remain scoped to the authenticated household. Foreign household record IDs cannot be used to access or mutate another household's data.
- **AC-06 Legacy preservation:** Existing users and their household financial data remain unchanged by new registrations.
- **AC-07 Concurrent registration:** Two simultaneous registrations with different emails both succeed and each receives a different household.
- **AC-08 Security:** Password hashing, normalized unique emails, session fixation protection, CSRF and login throttling remain in place.

## Out of scope

- Multiple households per user or switching between households.
- Automatic household creation for invited members (they join the inviter's household).
- Email verification, password recovery, social login or public cloud deployment.
- Changing the financial forecast model or synthetic reset-to-sample values.

## Verification evidence

- Backend unit and H2 integration tests cover repeated/concurrent registration, distinct household IDs, blank first-use data, invitation compatibility and household isolation.
- E2E authenticated fixtures register a distinct synthetic account per test.
