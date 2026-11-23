package com.finance.planner.household;

import java.time.Instant;

public record InvitationSummary(Long id, Instant createdAt, Instant expiresAt,
                                Instant revokedAt, Instant consumedAt) {
}
