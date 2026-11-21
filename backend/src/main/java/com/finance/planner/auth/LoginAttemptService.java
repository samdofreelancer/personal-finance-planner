package com.finance.planner.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Clock clock;
    private final Map<String, AttemptWindow> attempts = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, AttemptWindow> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void ensureAllowed(String key) {
        synchronized (attempts) {
            Instant now = clock.instant();
            pruneExpired(now);
            AttemptWindow window = attempts.get(key);
            if (window != null && window.failedAttempts() >= MAX_FAILED_ATTEMPTS) {
                throw new TooManyLoginAttemptsException();
            }
        }
    }

    public void recordFailure(String key) {
        synchronized (attempts) {
            Instant now = clock.instant();
            pruneExpired(now);
            AttemptWindow window = attempts.get(key);
            if (window == null || !window.expiresAt().isAfter(now)) {
                attempts.put(key, new AttemptWindow(1, now.plus(WINDOW)));
            } else {
                attempts.put(key, new AttemptWindow(window.failedAttempts() + 1, window.expiresAt()));
            }
        }
    }

    public void recordSuccess(String key) {
        synchronized (attempts) {
            attempts.remove(key);
        }
    }

    private void pruneExpired(Instant now) {
        attempts.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private record AttemptWindow(int failedAttempts, Instant expiresAt) {
    }
}
