package com.finance.planner.household;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BootstrapLockRepository extends JpaRepository<BootstrapLock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lock from BootstrapLock lock where lock.id = :id")
    Optional<BootstrapLock> findForUpdateById(@Param("id") Long id);
}
