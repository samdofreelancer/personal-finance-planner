package com.finance.planner.auth;

import com.finance.planner.household.HouseholdRole;

public record AuthResponse(Long userId, String email, Long householdId, HouseholdRole role) {
}
