package com.finance.planner.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T00:00:00Z");

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordResetDelivery delivery;
    @Mock private LoginAttemptService rateLimiter;
    @Mock private ObjectProvider<FindByIndexNameSessionRepository<?>> sessionRepositories;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        lenient().when(delivery.isConfigured()).thenReturn(true);
        lenient().when(sessionRepositories.getIfAvailable()).thenReturn(null);
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, delivery,
                rateLimiter, Clock.fixed(NOW, ZoneOffset.UTC), sessionRepositories,
                "https://planwise.example.test");
        user = new User("owner@example.com", passwordEncoder.encode("old-secure-password"));
        user.setId(12L);
    }

    @Test
    void requestCreatesOnlyAHashAndEmailsARandomTokenInTrustedConfiguredUrl() {
        when(userRepository.findByEmailForUpdate("owner@example.com")).thenReturn(Optional.of(user));

        service.requestReset(" OWNER@example.com ", "192.0.2.10");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq("owner@example.com"), linkCaptor.capture());

        String link = linkCaptor.getValue();
        URI uri = URI.create(link);
        String rawToken = java.net.URLDecoder.decode(uri.getRawQuery().substring("resetToken=".length()),
                java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("https://planwise.example.test/", uri.getScheme() + "://" + uri.getAuthority() + uri.getPath());
        assertEquals(43, rawToken.length());
        assertEquals(PasswordResetService.hashToken(rawToken), saved.getTokenHash());
        assertNotEquals(rawToken, saved.getTokenHash());
        assertEquals(NOW.plusSeconds(30 * 60), saved.getExpiresAt());
        verify(rateLimiter).recordFailure("192.0.2.10:password-reset");
        verify(rateLimiter).recordFailure("password-reset:owner@example.com");
    }

    @Test
    void unknownEmailDoesNotCreateTokenOrSendEmailButStillConsumesRateLimit() {
        when(userRepository.findByEmailForUpdate("unknown@example.com")).thenReturn(Optional.empty());

        service.requestReset("unknown@example.com", "192.0.2.10");

        verify(tokenRepository, never()).save(any());
        verify(delivery, never()).send(any(), any());
        verify(rateLimiter).recordFailure("192.0.2.10:password-reset");
        verify(rateLimiter).recordFailure("password-reset:unknown@example.com");
    }

    @Test
    void successfulResetChangesPasswordConsumesTokenAndInvalidatesOtherActiveTokens() {
        PasswordResetToken token = new PasswordResetToken(user, PasswordResetService.hashToken("reset-token"),
                NOW.plusSeconds(60), NOW);
        when(tokenRepository.findByTokenHashForUpdate(PasswordResetService.hashToken("reset-token")))
                .thenReturn(Optional.of(token));

        service.resetPassword("reset-token", "new-secure-password");

        assertTruePasswordMatches("new-secure-password");
        assertNotNull(token.getUsedAt());
        verify(tokenRepository).save(token);
        verify(tokenRepository).deleteAllByUser_IdAndUsedAtIsNull(12L);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void successfulResetDeletesEveryIndexedSessionForTheAccount() {
        FindByIndexNameSessionRepository sessionRepository = mock(FindByIndexNameSessionRepository.class);
        when(sessionRepositories.getIfAvailable()).thenReturn(sessionRepository);
        when(sessionRepository.findByPrincipalName("owner@example.com"))
                .thenReturn(Map.of("session-1", new MapSession("session-1"), "session-2", new MapSession("session-2")));
        PasswordResetToken token = new PasswordResetToken(user, PasswordResetService.hashToken("session-reset"),
                NOW.plusSeconds(60), NOW);
        when(tokenRepository.findByTokenHashForUpdate(PasswordResetService.hashToken("session-reset")))
                .thenReturn(Optional.of(token));

        service.resetPassword("session-reset", "another-secure-password");

        verify(sessionRepository).deleteById("session-1");
        verify(sessionRepository).deleteById("session-2");
        assertEquals("owner@example.com", userAsPrincipal().getUsername());
    }

    @Test
    void expiredAndAlreadyUsedTokensAreRejected() {
        PasswordResetToken expired = new PasswordResetToken(user, PasswordResetService.hashToken("expired"),
                NOW, NOW.minusSeconds(60));
        PasswordResetToken used = new PasswordResetToken(user, PasswordResetService.hashToken("used"),
                NOW.plusSeconds(60), NOW);
        used.setUsedAt(NOW.minusSeconds(1));
        when(tokenRepository.findByTokenHashForUpdate(PasswordResetService.hashToken("expired")))
                .thenReturn(Optional.of(expired));
        when(tokenRepository.findByTokenHashForUpdate(PasswordResetService.hashToken("used")))
                .thenReturn(Optional.of(used));

        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("expired", "new-secure-password"));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("used", "new-secure-password"));
        assertFalse(passwordEncoder.matches("new-secure-password", user.getPasswordHash()));
    }

    @Test
    void disabledEmailDeliveryDoesNotCreateAnUndeliverableToken() {
        when(delivery.isConfigured()).thenReturn(false);

        assertThrows(PasswordResetDeliveryUnavailableException.class,
                () -> service.requestReset("owner@example.com", "192.0.2.10"));

        verify(userRepository, never()).findByEmailForUpdate(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void rejectsUntrustedOrSubpathPublicUrlsInsteadOfBuildingResetLinksFromRequestHeaders() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordResetService(userRepository, tokenRepository,
                passwordEncoder, delivery, rateLimiter, Clock.fixed(NOW, ZoneOffset.UTC), sessionRepositories,
                "http://planwise.example.test"));
        assertThrows(IllegalArgumentException.class, () -> new PasswordResetService(userRepository, tokenRepository,
                passwordEncoder, delivery, rateLimiter, Clock.fixed(NOW, ZoneOffset.UTC), sessionRepositories,
                "https://planwise.example.test/app"));
    }

    @Test
    void rejectsPasswordsOutsideTheExistingBcryptPolicy() {
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("unused", "short"));
        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("unused", "😀".repeat(19)));
        verify(tokenRepository, never()).findByTokenHashForUpdate(any());
    }

    private void assertTruePasswordMatches(String rawPassword) {
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordHash()));
    }

    private AuthenticatedUser userAsPrincipal() {
        return new AuthenticatedUser(user.getId(), user.getEmail(), 7L,
                com.finance.planner.household.HouseholdRole.OWNER);
    }
}
