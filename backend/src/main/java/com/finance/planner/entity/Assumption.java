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

    @Column(name = "january_multiplier", nullable = false, precision = 5, scale = 2) private BigDecimal januaryMultiplier = new BigDecimal("2.0");
    @Column(name = "starting_savings", nullable = false) private Long startingSavings = 30_000_000L;
    @Column(name = "forecast_years", nullable = false) private Integer forecastYears = 4;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
