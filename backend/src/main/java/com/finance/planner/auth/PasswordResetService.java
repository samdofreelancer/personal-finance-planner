package com.finance.planner.auth;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetDelivery delivery;
    private final LoginAttemptService rateLimiter;
    private final Clock clock;
    private final ObjectProvider<FindByIndexNameSessionRepository<?>> sessionRepositories;
    private final String publicBaseUrl;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder, PasswordResetDelivery delivery,
                                LoginAttemptService rateLimiter, Clock clock,
                                ObjectProvider<FindByIndexNameSessionRepository<?>> sessionRepositories,
                                @Value("${app.public-base-url:http://localhost}") String publicBaseUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.delivery = delivery;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.sessionRepositories = sessionRepositories;
        this.publicBaseUrl = validatePublicBaseUrl(publicBaseUrl);
    }

    @Transactional
    public void requestReset(String email, String remoteAddress) {
        if (!delivery.isConfigured()) throw new PasswordResetDeliveryUnavailableException();

        String normalizedEmail;
        try {
            normalizedEmail = AuthService.normalizeEmail(email);
        } catch (IllegalArgumentException exception) {
            return;
        }

        String ipRateLimitKey = remoteAddress + ":password-reset";
        String emailRateLimitKey = "password-reset:" + normalizedEmail;
        try {
            rateLimiter.ensureAllowed(ipRateLimitKey);
            rateLimiter.ensureAllowed(emailRateLimitKey);
        } catch (TooManyLoginAttemptsException exception) {
            return;
        }
        rateLimiter.recordFailure(ipRateLimitKey);
        rateLimiter.recordFailure(emailRateLimitKey);

        User user = userRepository.findByEmailForUpdate(normalizedEmail).orElse(null);
        if (user == null) return;

        tokenRepository.deleteAllByUser_IdAndUsedAtIsNull(user.getId());
        String rawToken = generateToken();
        Instant now = clock.instant();
        tokenRepository.save(new PasswordResetToken(user, hashToken(rawToken), now.plus(TOKEN_LIFETIME), now));

        String resetLink = UriComponentsBuilder.fromUriString(publicBaseUrl)
                .replacePath("/")
                .replaceQuery(null)
                .fragment(null)
                .queryParam("resetToken", rawToken)
                .build()
                .encode()
                .toUriString();
        try {
            delivery.send(normalizedEmail, resetLink);
        } catch (RuntimeException exception) {
            tokenRepository.deleteAllByUser_IdAndUsedAtIsNull(user.getId());
            log.warn("Password recovery email delivery failed for a request; account details and token omitted.");
            throw new PasswordResetDeliveryFailedException();
        }
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validateNewPassword(newPassword);
        PasswordResetToken token = tokenRepository.findByTokenHashForUpdate(hashToken(rawToken))
                .orElseThrow(PasswordResetService::invalidToken);
        Instant now = clock.instant();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) throw invalidToken();

        User user = token.getUser();
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(now);
        tokenRepository.save(token);
        tokenRepository.deleteAllByUser_IdAndUsedAtIsNull(user.getId());
        invalidateSessions(user.getEmail());
    }

    static String hashToken(String token) {
        if (token == null || token.isBlank()) throw invalidToken();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void invalidateSessions(String email) {
        FindByIndexNameSessionRepository<?> repository = sessionRepositories.getIfAvailable();
        if (repository == null) return;
        Map<String, ?> sessions = repository.findByPrincipalName(email);
        sessions.keySet().forEach(repository::deleteById);
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void validateNewPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 12 ký tự và tối đa 72 byte UTF-8.");
        }
    }

    private static String validatePublicBaseUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()))
                    || !("https".equalsIgnoreCase(uri.getScheme())
                    || ("http".equalsIgnoreCase(uri.getScheme()) && "localhost".equalsIgnoreCase(uri.getHost())))) {
                throw new IllegalArgumentException("APP_PUBLIC_BASE_URL must be HTTPS, or HTTP localhost for development.");
            }
            return uri.toString().replaceAll("/+$", "");
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("APP_PUBLIC_BASE_URL must be HTTPS, or HTTP localhost for development.");
        }
    }

    private static IllegalArgumentException invalidToken() {
        return new IllegalArgumentException("Liên kết khôi phục không hợp lệ hoặc đã hết hạn.");
    }
}
