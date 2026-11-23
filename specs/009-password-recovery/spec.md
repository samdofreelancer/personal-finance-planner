# 009 — Self-service password recovery

## Status

Accepted for implementation — 2026-11-23.

## Goal

Allow an account owner to regain access when they have forgotten their password, without exposing whether an email address is registered and without weakening the existing session-based authentication model.

## Product and security decisions

1. A user requests recovery with their account email. The API returns the same generic success response for existing and unknown accounts.
2. A recovery token is generated from 256 bits of cryptographically secure randomness. Only its SHA-256 hash is persisted.
3. A token is valid for 30 minutes, can be consumed once, and is invalidated when a newer token is issued for the same user.
4. The raw token is sent only in an email link. It must never be logged, stored in plaintext, or returned by an API.
5. Requests are rate-limited using the existing Redis-backed authentication throttling service.
6. Reset requires a password of at least 12 characters and no more than 72 UTF-8 bytes, matching registration and password-change policy.
7. Successful reset replaces the BCrypt password hash and invalidates the user's existing sessions. It does not change household membership or financial data.
8. Email delivery uses configurable SMTP settings. When SMTP is not configured, recovery is explicitly unavailable; the API must not create a token that cannot be delivered.
9. The public application URL used in email links comes from trusted server configuration, never the request Host header.

## Acceptance criteria

- **AC-01 Enumeration resistance:** Existing and unknown valid email addresses receive the same HTTP status and response body.
- **AC-02 Token secrecy:** Tokens have at least 256 bits of entropy; persistence contains only SHA-256 hashes.
- **AC-03 Expiry and one-time use:** Expired, consumed, superseded and unknown tokens cannot change a password.
- **AC-04 Password policy:** New password validation matches registration and authenticated password change.
- **AC-05 Session revocation:** Successful recovery invalidates all existing sessions for the account.
- **AC-06 Throttling:** Repeated requests are throttled through Redis without disclosing account existence.
- **AC-07 Email delivery:** SMTP settings and trusted public base URL are configurable through environment variables; no token is created when delivery is disabled or fails.
- **AC-08 Frontend flow:** Login links to recovery; email submission shows generic confirmation; reset links allow a new password and confirmation; expired/invalid tokens show an actionable error.
- **AC-09 Data integrity:** Password recovery does not mutate user email, household membership or financial aggregates.
- **AC-10 CSRF:** Both state-changing endpoints remain protected by the existing CSRF mechanism.

## Out of scope

- Email verification during registration.
- Changing email addresses, MFA, passkeys or social login.
- A "remember me" session policy.

## Operational prerequisite

SMTP host, port, username, password, sender address and the externally reachable application base URL must be configured in the deployment environment before password recovery can send real email.
