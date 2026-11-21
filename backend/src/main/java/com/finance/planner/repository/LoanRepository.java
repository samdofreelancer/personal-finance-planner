package com.finance.planner.repository;

import com.finance.planner.entity.Loan;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    @EntityGraph(attributePaths = "repayments")
    List<Loan> findAllByHouseholdIdOrderByIdAsc(Long householdId);
}
