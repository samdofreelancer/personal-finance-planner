package com.finance.planner.auth;

import com.finance.planner.household.BootstrapLockRepository;
import com.finance.planner.household.Household;
import com.finance.planner.household.HouseholdInvitation;
import com.finance.planner.household.HouseholdInvitationRepository;
import com.finance.planner.household.HouseholdMember;
import com.finance.planner.household.HouseholdMemberRepository;
import com.finance.planner.household.HouseholdRepository;
import com.finance.planner.household.HouseholdRole;
import com.finance.planner.household.InvitationToken;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final HouseholdInvitationRepository invitationRepository;
    private final BootstrapLockRepository bootstrapLockRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, HouseholdRepository householdRepository,
                       HouseholdMemberRepository householdMemberRepository,
                       HouseholdInvitationRepository invitationRepository,
                       BootstrapLockRepository bootstrapLockRepository,
                       PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.invitationRepository = invitationRepository;
        this.bootstrapLockRepository = bootstrapLockRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-user-password");
    }

    @Transactional
    public AuthResponse registerInitial(RegisterRequest request) {
        bootstrapLockRepository.findForUpdateById(1L)
                .orElseThrow(() -> new IllegalStateException("Initial registration is not available"));

        if (userRepository.count() > 0) {
            throw new IllegalStateException("Initial registration is closed; an invitation is required");
        }

        String email = normalizeEmail(request.email());
        validatePassword(request.password());
        ensureEmailAvailable(email);
        Household household = householdRepository.findFirstByInitialHouseholdTrue()
                .orElseThrow(() -> new IllegalStateException("Initial household has not been provisioned"));

        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password())));
        HouseholdMember membership = householdMemberRepository.save(
                new HouseholdMember(household, user, HouseholdRole.OWNER));
        return toResponse(user, membership);
    }

    @Transactional
    public AuthResponse registerWithInvitation(RegisterRequest request, String rawToken) {
        String email = normalizeEmail(request.email());
        validatePassword(request.password());
        ensureEmailAvailable(email);

        String tokenHash = InvitationToken.hash(rawToken);
        HouseholdInvitation invitation = invitationRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invitation is invalid or expired"));
        Instant now = clock.instant();
        if (!invitation.getExpiresAt().isAfter(now)
                || invitation.getRevokedAt() != null
                || invitation.getConsumedAt() != null) {
            throw new IllegalArgumentException("Invitation is invalid or expired");
        }

        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password())));
        HouseholdMember membership = householdMemberRepository.save(
                new HouseholdMember(invitation.getHousehold(), user, HouseholdRole.MEMBER));
        invitation.setConsumedAt(now);
        invitationRepository.save(invitation);
        return toResponse(user, membership);
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser authenticate(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email).orElse(null);
        String password = request.password() == null ? "" : request.password();
        if (user == null || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            passwordEncoder.matches("not-a-real-user-password", dummyPasswordHash);
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        HouseholdMember membership = householdMemberRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User has no household membership"));
        return new AuthenticatedUser(user.getId(), user.getEmail(), membership.getHousehold().getId(), membership.getRole());
    }

    @Transactional(readOnly = true)
    public boolean isInitialRegistrationAvailable() {
        return userRepository.count() == 0;
    }

    static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("A valid email is required");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Password must be at least 12 characters and at most 72 UTF-8 bytes");
        }
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
    }

    private AuthResponse toResponse(User user, HouseholdMember membership) {
        return new AuthResponse(user.getId(), user.getEmail(), membership.getHousehold().getId(), membership.getRole());
    }
}
