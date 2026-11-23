package com.finance.planner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "income_amount_changes", uniqueConstraints =
        @UniqueConstraint(name = "uk_income_change_month", columnNames = {"assumption_id", "effective_month"}))
@Getter
@Setter
public class IncomeAmountChange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assumption_id", nullable = false)
    private Assumption assumption;

    @Column(name = "effective_month", nullable = false)
    private LocalDate effectiveMonth;

    @Column(nullable = false)
    private Long amount;
}
