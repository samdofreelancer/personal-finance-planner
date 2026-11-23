# Feature 008 — Personal Profile

## Goal

Give every authenticated Planwise account a dedicated personal profile, separate from its household workspace and financial assumptions.

## User stories

- As a signed-in user, I can view my account email and account creation date.
- As a signed-in user, I can set a display name and optional avatar URL.
- As a signed-in user, I can change my password after proving knowledge of the current password.
- As a signed-in user, I can access my profile independently from household settings.

## Functional requirements

1. Profile reads and writes are scoped to the authenticated user principal; request payloads never accept a target user ID.
2. Email and account creation timestamp are read-only.
3. Display name is required on update and limited to 80 characters.
4. Avatar URL is optional, limited to 2048 characters and must be an HTTPS URL without embedded user information.
5. Password changes require the current password; the new password must contain at least 12 characters, use no more than 72 UTF-8 bytes, and differ from the current password.
6. Existing users are migrated without losing accounts or sessions; profile fields begin nullable and can be completed after sign-in.
7. Personal profile identity is not household data and must not alter financial profile or forecast calculations.

## Acceptance criteria

- An authenticated user can read and update only their own profile.
- Unauthenticated profile requests are rejected.
- Password hash is never returned by profile APIs.
- Invalid avatar URLs, blank/oversized display names, incorrect current passwords and weak new passwords are rejected with actionable API errors.
- The profile screen is accessible from the account drawer on desktop and mobile.
- Household settings remain a separate screen and continue to manage invitations only.
- Frontend unit tests, backend unit/integration tests and production build pass.

## Out of scope

- Avatar binary upload/storage, image cropping, and profile discovery by other users.
- Email change, account deletion, MFA, and cross-device session revocation.
