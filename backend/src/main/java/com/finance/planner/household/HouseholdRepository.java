package com.finance.planner.household;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseholdRepository extends JpaRepository<Household, Long> {
    Optional<Household> findFirstByInitialHouseholdTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select household from Household household where household.id = :id")
    Optional<Household> findForUpdateById(@Param("id") Long id);
}
