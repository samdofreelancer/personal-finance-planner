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
    @Column(name = "january_multiplier", nullable = false, precision = 5, scale = 2) private BigDecimal januaryMultiplier = new BigDecimal("2.0");
    @Column(name = "starting_savings", nullable = false) private Long startingSavings = 30_000_000L;
    @Column(name = "raise_2027", nullable = false, precision = 5, scale = 4) private BigDecimal raise2027 = new BigDecimal("0.08");
    @Column(name = "raise_2028", nullable = false, precision = 5, scale = 4) private BigDecimal raise2028 = new BigDecimal("0.08");
    @Column(name = "raise_2029", nullable = false, precision = 5, scale = 4) private BigDecimal raise2029 = new BigDecimal("0.08");
    @Column(name = "raise_2030", nullable = false, precision = 5, scale = 4) private BigDecimal raise2030 = new BigDecimal("0.08");
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
