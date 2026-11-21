package com.finance.planner.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finance.planner.household.BootstrapLock;
import com.finance.planner.household.BootstrapLockRepository;
import com.finance.planner.household.Household;
import com.finance.planner.household.HouseholdInvitation;
import com.finance.planner.household.HouseholdInvitationRepository;
import com.finance.planner.household.HouseholdMember;
import com.finance.planner.household.HouseholdMemberRepository;
import com.finance.planner.household.HouseholdRepository;
import com.finance.planner.household.HouseholdRole;
import com.finance.planner.household.InvitationToken;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private HouseholdRepository householdRepository;
    @Mock private HouseholdMemberRepository householdMemberRepository;
    @Mock private HouseholdInvitationRepository invitationRepository;
    @Mock private BootstrapLockRepository bootstrapLockRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private Household initialHousehold;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(userRepository, householdRepository, householdMemberRepository,
                invitationRepository, bootstrapLockRepository, passwordEncoder,
                Clock.fixed(Instant.parse("2026-11-21T00:00:00Z"), ZoneOffset.UTC));
        initialHousehold = new Household("Gia đình", true);
        initialHousehold.setId(1L);
    }

    @Test
    void initialRegistrationNormalizesEmailHashesPasswordAndCreatesOwnerMembership() {
        when(bootstrapLockRepository.findForUpdateById(1L)).thenReturn(Optional.of(new BootstrapLock(1L)));
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("owner@example.com")).thenReturn(false);
        when(householdRepository.findFirstByInitialHouseholdTrue()).thenReturn(Optional.of(initialHousehold));
        when(householdMemberRepository.save(any(HouseholdMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        AuthResponse response = authService.registerInitial(new RegisterRequest("  OWNER@Example.com ", "correct-horse-battery-staple"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("owner@example.com", savedUser.getEmail());
        assertNotEquals("correct-horse-battery-staple", savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches("correct-horse-battery-staple", savedUser.getPasswordHash()));

        ArgumentCaptor<HouseholdMember> memberCaptor = ArgumentCaptor.forClass(HouseholdMember.class);
        verify(householdMemberRepository).save(memberCaptor.capture());
        assertEquals(HouseholdRole.OWNER, memberCaptor.getValue().getRole());
        assertEquals(1L, memberCaptor.getValue().getHousehold().getId());
        assertEquals("owner@example.com", response.email());
        assertEquals(HouseholdRole.OWNER, response.role());
    }

    @Test
    void registrationWithInvitationConsumesTokenAndCreatesMember() {
        HouseholdInvitation invitation = new HouseholdInvitation(initialHousehold, null,
                InvitationToken.hash("single-use-token"), Instant.parse("2026-11-22T00:00:00Z"));
        invitation.setId(20L);
        when(invitationRepository.findByTokenHashForUpdate(InvitationToken.hash("single-use-token")))
                .thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmail("member@example.com")).thenReturn(false);
        when(householdMemberRepository.save(any(HouseholdMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocationCall -> {
            User user = invocationCall.getArgument(0);
            user.setId(11L);
            return user;
        });

        AuthResponse response = authService.registerWithInvitation(
                new RegisterRequest("MEMBER@example.com", "correct-horse-battery-staple"), "single-use-token");

        assertEquals("member@example.com", response.email());
        assertEquals(HouseholdRole.MEMBER, response.role());
        assertNotNull(invitation.getConsumedAt());
        verify(invitationRepository).save(invitation);
    }

    @Test
    void rejectsInitialRegistrationAfterBootstrapHasBeenClaimed() {
        when(bootstrapLockRepository.findForUpdateById(1L)).thenReturn(Optional.of(new BootstrapLock(1L)));
        when(userRepository.count()).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> authService.registerInitial(
                new RegisterRequest("another@example.com", "correct-horse-battery-staple")));
    }

    @Test
    void authenticatesNormalizedEmailAndReturnsTheUsersHouseholdRole() {
        User user = new User("owner@example.com", passwordEncoder.encode("correct-horse-battery-staple"));
        user.setId(10L);
        HouseholdMember membership = new HouseholdMember(initialHousehold, user, HouseholdRole.OWNER);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(householdMemberRepository.findByUserId(10L)).thenReturn(Optional.of(membership));

        AuthenticatedUser authenticated = authService.authenticate(
                new LoginRequest(" OWNER@example.com ", "correct-horse-battery-staple"));

        assertEquals(10L, authenticated.userId());
        assertEquals(1L, authenticated.householdId());
        assertEquals(HouseholdRole.OWNER, authenticated.role());
    }

    @Test
    void rejectsWrongPasswordWithGenericAuthenticationFailure() {
        User user = new User("owner@example.com", passwordEncoder.encode("correct-horse-battery-staple"));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.authenticate(new LoginRequest("owner@example.com", "wrong-password")));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void rejectsUnknownEmailWithTheSameGenericAuthenticationFailure() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.authenticate(new LoginRequest("unknown@example.com", "wrong-password")));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void rejectsPasswordsLongerThanBcryptUtf8LimitAsGenericBadCredentials() {
        User user = new User("owner@example.com", passwordEncoder.encode("correct-horse-battery-staple"));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(
                new LoginRequest("owner@example.com", "😀".repeat(20))));
    }

    @Test
    void rejectsWeakPasswordBeforePersistingAnInitialAccount() {
        when(bootstrapLockRepository.findForUpdateById(1L)).thenReturn(Optional.of(new BootstrapLock(1L)));
        when(userRepository.count()).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> authService.registerInitial(
                new RegisterRequest("owner@example.com", "short")));

        verifyNoInteractions(householdMemberRepository);
    }

    @Test
    void rejectsAnExistingEmailAfterNormalization() {
        when(bootstrapLockRepository.findForUpdateById(1L)).thenReturn(Optional.of(new BootstrapLock(1L)));
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("owner@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.registerInitial(
                new RegisterRequest(" OWNER@EXAMPLE.COM ", "correct-horse-battery-staple")));

        verify(userRepository).existsByEmail("owner@example.com");
    }

    @Test
    void rejectsExpiredRevokedAndConsumedInvitations() {
        HouseholdInvitation expired = invitation("expired-token", Instant.parse("2026-11-21T00:00:00Z"));
        HouseholdInvitation revoked = invitation("revoked-token", Instant.parse("2026-11-22T00:00:00Z"));
        revoked.setRevokedAt(Instant.parse("2026-11-20T00:00:00Z"));
        HouseholdInvitation consumed = invitation("consumed-token", Instant.parse("2026-11-22T00:00:00Z"));
        consumed.setConsumedAt(Instant.parse("2026-11-20T00:00:00Z"));
        when(invitationRepository.findByTokenHashForUpdate(InvitationToken.hash("expired-token"))).thenReturn(Optional.of(expired));
        when(invitationRepository.findByTokenHashForUpdate(InvitationToken.hash("revoked-token"))).thenReturn(Optional.of(revoked));
        when(invitationRepository.findByTokenHashForUpdate(InvitationToken.hash("consumed-token"))).thenReturn(Optional.of(consumed));
        when(userRepository.existsByEmail(any(String.class))).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.registerWithInvitation(
                new RegisterRequest("expired@example.com", "correct-horse-battery-staple"), "expired-token"));
        assertThrows(IllegalArgumentException.class, () -> authService.registerWithInvitation(
                new RegisterRequest("revoked@example.com", "correct-horse-battery-staple"), "revoked-token"));
        assertThrows(IllegalArgumentException.class, () -> authService.registerWithInvitation(
                new RegisterRequest("consumed@example.com", "correct-horse-battery-staple"), "consumed-token"));
    }

    private HouseholdInvitation invitation(String token, Instant expiresAt) {
        return new HouseholdInvitation(initialHousehold, null, InvitationToken.hash(token), expiresAt);
    }
}
