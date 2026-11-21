package com.finance.planner.household;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

    private static final Duration DEFAULT_VALIDITY = Duration.ofDays(7);
    private static final Duration MAX_VALIDITY = Duration.ofDays(30);

    private final HouseholdInvitationRepository invitationRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final Clock clock;

    public InvitationService(HouseholdInvitationRepository invitationRepository,
                             HouseholdMemberRepository householdMemberRepository, Clock clock) {
        this.invitationRepository = invitationRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.clock = clock;
    }

    @Transactional
    public CreatedInvitation createInvitation(HouseholdContext context, Duration validity) {
        requireOwner(context);
        if (context.householdId() == null || context.userId() == null) {
            throw new AccessDeniedException("Invitation creation is not permitted");
        }
        HouseholdMember ownerMembership = householdMemberRepository.findByUserId(context.userId())
                .orElseThrow(() -> new AccessDeniedException("Invitation creation is not permitted"));
        if (ownerMembership.getRole() != HouseholdRole.OWNER
                || !context.householdId().equals(ownerMembership.getHousehold().getId())) {
            throw new AccessDeniedException("Invitation creation is not permitted");
        }

        Duration effectiveValidity = validity == null ? DEFAULT_VALIDITY : validity;
        if (effectiveValidity.isZero() || effectiveValidity.isNegative() || effectiveValidity.compareTo(MAX_VALIDITY) > 0) {
            throw new IllegalArgumentException("Invitation validity must be between 1 second and 30 days");
        }

        String rawToken = InvitationToken.generate();
        Instant expiresAt = clock.instant().plus(effectiveValidity);
        HouseholdInvitation invitation = invitationRepository.save(
                new HouseholdInvitation(ownerMembership.getHousehold(), ownerMembership.getUser(),
                        InvitationToken.hash(rawToken), expiresAt));
        return new CreatedInvitation(invitation.getId(), rawToken, expiresAt);
    }

    @Transactional
    public void revoke(HouseholdContext context, Long invitationId) {
        requireOwner(context);
        HouseholdInvitation invitation = invitationRepository.findByIdAndHouseholdIdForUpdate(invitationId, context.householdId())
                .orElseThrow(() -> new IllegalArgumentException("Invitation was not found"));
        if (invitation.getConsumedAt() != null) {
            throw new IllegalStateException("A consumed invitation cannot be revoked");
        }
        if (invitation.getRevokedAt() == null) {
            invitation.setRevokedAt(clock.instant());
            invitationRepository.save(invitation);
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<InvitationSummary> listInvitations(HouseholdContext context) {
        requireOwner(context);
        if (context.householdId() == null) {
            throw new AccessDeniedException("Invitation listing is not permitted");
        }
        return invitationRepository.findAllByHouseholdIdOrderByCreatedAtDesc(context.householdId()).stream()
                .map(invitation -> new InvitationSummary(invitation.getId(), invitation.getCreatedAt(),
                        invitation.getExpiresAt(), invitation.getRevokedAt(), invitation.getConsumedAt()))
                .toList();
    }

    private void requireOwner(HouseholdContext context) {
        if (context == null || context.role() != HouseholdRole.OWNER) {
            throw new AccessDeniedException("Only a household owner can manage invitations");
        }
    }
}
