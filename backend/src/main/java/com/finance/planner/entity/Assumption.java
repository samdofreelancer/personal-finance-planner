package com.finance.planner.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "assumptions")
@Data
public class Assumption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_salary", nullable = false) private Long baseSalary = 42_000_000L;
    @Column(name = "january_multiplier", nullable = false, precision = 5, scale = 2) private BigDecimal januaryMultiplier = new BigDecimal("2.5");
    @Column(name = "living", nullable = false) private Long living = 4_250_000L;
    @Column(name = "child_through_2026", nullable = false) private Long childThrough2026 = 2_500_000L;
    @Column(name = "child_q1_2027", nullable = false) private Long childQ12027 = 2_750_000L;
    @Column(name = "child_from_apr_2027", nullable = false) private Long childFromApr2027 = 3_250_000L;
    @Column(name = "rent_through_jan_2027", nullable = false) private Long rentThroughJan2027 = 3_750_000L;
    @Column(name = "rent_from_feb_2027", nullable = false) private Long rentFromFeb2027 = 5_750_000L;
    @Column(name = "loan_principal", nullable = false) private Long loanPrincipal = 120_000_000L;
    @Column(name = "fixed_payment", nullable = false) private Long fixedPayment = 10_000_000L;
    @Column(name = "nov_interest", nullable = false) private Long novInterest = 1_200_000L;
    @Column(name = "dec_interest", nullable = false) private Long decInterest = 1_100_000L;
    @Column(name = "payoff_interest") private Long payoffInterest = 1_000_000L;
    @Column(name = "payoff_interest_confirmed", nullable = false) private Boolean payoffInterestConfirmed = false;
    @Column(name = "starting_savings", nullable = false) private Long startingSavings = 30_000_000L;
    @Column(name = "raise_2027", nullable = false, precision = 5, scale = 4) private BigDecimal raise2027 = new BigDecimal("0.08");
    @Column(name = "raise_2028", nullable = false, precision = 5, scale = 4) private BigDecimal raise2028 = new BigDecimal("0.08");
    @Column(name = "raise_2029", nullable = false, precision = 5, scale = 4) private BigDecimal raise2029 = new BigDecimal("0.08");
    @Column(name = "raise_2030", nullable = false, precision = 5, scale = 4) private BigDecimal raise2030 = new BigDecimal("0.08");
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
