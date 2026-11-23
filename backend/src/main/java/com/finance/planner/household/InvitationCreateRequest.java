package com.finance.planner.household;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record InvitationCreateRequest(@Min(1) @Max(30) Integer validityDays) {
}
