package com.finance.planner.household;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.finance.planner.auth.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HouseholdServiceTest {

    @Mock private HouseholdMemberRepository householdMemberRepository;
    @InjectMocks private HouseholdService householdService;

    @Test
    void resolvesHouseholdContextFromPersistedMembership() {
        Household household = new Household("Gia đình", true);
        household.setId(7L);
        User user = new User("member@example.com", "hash");
        user.setId(11L);
        when(householdMemberRepository.findByUserId(11L))
                .thenReturn(Optional.of(new HouseholdMember(household, user, HouseholdRole.MEMBER)));

        HouseholdContext context = householdService.requireContextForUser(11L);

        assertEquals(new HouseholdContext(7L, 11L, HouseholdRole.MEMBER), context);
    }

    @Test
    void rejectsUsersWithoutHouseholdMembership() {
        when(householdMemberRepository.findByUserId(11L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> householdService.requireContextForUser(11L));
    }
}
