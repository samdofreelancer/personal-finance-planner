package com.finance.planner.repository;

import com.finance.planner.entity.ExpenseCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    List<ExpenseCategory> findAllByHouseholdIdOrderByIdAsc(Long householdId);
}
