package com.finance.planner.auth;

import java.time.Instant;

public record ProfileResponse(Long userId, String email, String displayName, String avatarUrl, Instant createdAt) {
}
