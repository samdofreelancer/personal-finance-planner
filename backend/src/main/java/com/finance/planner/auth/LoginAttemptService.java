package com.finance.planner.auth;

/** Shared contract for tracking failed login attempts. */
public interface LoginAttemptService {

    void ensureAllowed(String key);

    void recordFailure(String key);

    void recordSuccess(String key);
}
