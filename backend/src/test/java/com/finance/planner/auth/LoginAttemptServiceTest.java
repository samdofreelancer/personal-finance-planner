package com.finance.planner.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(Clock.fixed(Instant.parse("2026-11-21T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void limitsAnIdentifierAfterFiveFailedAttemptsWithinTheWindow() {
        String key = "127.0.0.1:owner@example.com";

        for (int attempt = 0; attempt < 5; attempt++) {
            assertDoesNotThrow(() -> service.ensureAllowed(key));
            service.recordFailure(key);
        }

        assertThrows(TooManyLoginAttemptsException.class, () -> service.ensureAllowed(key));
    }

    @Test
    void successfulLoginResetsTheFailureWindow() {
        String key = "127.0.0.1:owner@example.com";
        for (int attempt = 0; attempt < 5; attempt++) {
            service.recordFailure(key);
        }

        service.recordSuccess(key);

        assertDoesNotThrow(() -> service.ensureAllowed(key));
    }
}
