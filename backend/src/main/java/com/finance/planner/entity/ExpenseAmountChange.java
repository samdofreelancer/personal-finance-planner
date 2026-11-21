package com.finance.planner.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "expense_amount_changes", uniqueConstraints =
        @UniqueConstraint(name = "uk_expense_change_month", columnNames = {"expense_category_id", "effective_month"}))
@Getter
@Setter
public class ExpenseAmountChange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_category_id", nullable = false)
    private ExpenseCategory category;

    @Column(name = "effective_month", nullable = false)
    private LocalDate effectiveMonth;

    @Column(nullable = false)
    private Long amount;
}
