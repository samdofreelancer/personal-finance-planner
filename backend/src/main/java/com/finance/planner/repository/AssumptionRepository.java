package com.finance.planner.repository;

import com.finance.planner.entity.Assumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssumptionRepository extends JpaRepository<Assumption, Long> {
    Assumption findTopByOrderByIdAsc();
}