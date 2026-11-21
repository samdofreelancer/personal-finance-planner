package com.finance.planner.household;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseholdInvitationRepository extends JpaRepository<HouseholdInvitation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from HouseholdInvitation invitation where invitation.tokenHash = :tokenHash")
    Optional<HouseholdInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<HouseholdInvitation> findByIdAndHouseholdId(Long id, Long householdId);

    java.util.List<HouseholdInvitation> findAllByHouseholdIdOrderByCreatedAtDesc(Long householdId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from HouseholdInvitation invitation where invitation.id = :id and invitation.household.id = :householdId")
    Optional<HouseholdInvitation> findByIdAndHouseholdIdForUpdate(@Param("id") Long id,
                                                                  @Param("householdId") Long householdId);
}
