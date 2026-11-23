package com.finance.planner.security;

import com.finance.planner.auth.AuthenticatedUser;
import com.finance.planner.household.HouseholdContext;
import com.finance.planner.household.HouseholdService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class HouseholdContextResolver {

    private final HouseholdService householdService;

    public HouseholdContextResolver(HouseholdService householdService) {
        this.householdService = householdService;
    }

    public HouseholdContext requireContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated household context is required");
        }
        return householdService.requireContextForUser(principal.userId());
    }
}
