package com.finance.planner.household;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.planner.auth.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock private HouseholdInvitationRepository invitationRepository;
    @Mock private HouseholdMemberRepository householdMemberRepository;

    private InvitationService invitationService;
    private Household household;
    private User owner;

    @BeforeEach
    void setUp() {
        invitationService = new InvitationService(invitationRepository, householdMemberRepository,
                Clock.fixed(Instant.parse("2026-11-21T00:00:00Z"), ZoneOffset.UTC));
        household = new Household("Gia đình", true);
        household.setId(1L);
        owner = new User("owner@example.com", "hash");
        owner.setId(10L);
    }

    @Test
    void createsRandomSingleUseInvitationAndPersistsOnlyItsHash() {
        HouseholdContext ownerContext = new HouseholdContext(1L, 10L, HouseholdRole.OWNER);
        when(householdMemberRepository.findByUserId(10L))
                .thenReturn(Optional.of(new HouseholdMember(household, owner, HouseholdRole.OWNER)));
        when(invitationRepository.save(any(HouseholdInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreatedInvitation created = invitationService.createInvitation(ownerContext, Duration.ofDays(7));

        ArgumentCaptor<HouseholdInvitation> captor = ArgumentCaptor.forClass(HouseholdInvitation.class);
        verify(invitationRepository).save(captor.capture());
        HouseholdInvitation persisted = captor.getValue();
        assertNotEquals(created.token(), persisted.getTokenHash());
        assertEquals(InvitationToken.hash(created.token()), persisted.getTokenHash());
        assertEquals(64, persisted.getTokenHash().length());
        assertEquals(43, created.token().length());
        assertEquals(Instant.parse("2026-11-28T00:00:00Z"), created.expiresAt());
    }

    @Test
    void memberCannotCreateHouseholdInvitations() {
        HouseholdContext memberContext = new HouseholdContext(1L, 11L, HouseholdRole.MEMBER);

        assertThrows(AccessDeniedException.class,
                () -> invitationService.createInvitation(memberContext, Duration.ofDays(7)));
    }

    @Test
    void revokesOnlyAnInvitationOwnedByTheCurrentHousehold() {
        HouseholdContext ownerContext = new HouseholdContext(1L, 10L, HouseholdRole.OWNER);
        when(invitationRepository.findByIdAndHouseholdIdForUpdate(50L, 1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> invitationService.revoke(ownerContext, 50L));
    }

    @Test
    void ownerCanRevokeAnUnconsumedInvitationInTheirHousehold() {
        HouseholdContext ownerContext = new HouseholdContext(1L, 10L, HouseholdRole.OWNER);
        HouseholdInvitation invitation = new HouseholdInvitation(household, owner, "token-hash",
                Instant.parse("2026-11-28T00:00:00Z"));
        when(invitationRepository.findByIdAndHouseholdIdForUpdate(50L, 1L)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(invitation)).thenReturn(invitation);

        invitationService.revoke(ownerContext, 50L);

        assertNotNull(invitation.getRevokedAt());
        verify(invitationRepository).save(invitation);
    }
}
