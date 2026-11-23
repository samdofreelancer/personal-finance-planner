package com.finance.planner.repository;

import com.finance.planner.entity.IncomeAmountChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeAmountChangeRepository extends JpaRepository<IncomeAmountChange, Long> {
    List<IncomeAmountChange> findAllByAssumptionIdOrderByEffectiveMonthAsc(Long assumptionId);
    void deleteAllByAssumptionId(Long assumptionId);
}
