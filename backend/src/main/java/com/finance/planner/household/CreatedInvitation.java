package com.finance.planner.household;

import java.time.Instant;

public record CreatedInvitation(Long id, String token, Instant expiresAt) {
}
