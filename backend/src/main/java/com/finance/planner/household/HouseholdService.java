package com.finance.planner.household;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HouseholdService {

    private final HouseholdMemberRepository householdMemberRepository;

    public HouseholdService(HouseholdMemberRepository householdMemberRepository) {
        this.householdMemberRepository = householdMemberRepository;
    }

    @Transactional(readOnly = true)
    public HouseholdContext requireContextForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        HouseholdMember membership = householdMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("User has no household membership"));
        return new HouseholdContext(membership.getHousehold().getId(), userId, membership.getRole());
    }
}
