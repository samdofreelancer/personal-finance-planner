package com.finance.planner.auth;

import com.finance.planner.household.HouseholdRole;

public record AuthenticatedUser(Long userId, String email, Long householdId, HouseholdRole role) {
}
