package com.finance.planner.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.finance.planner.dto.AssumptionDTO;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidationServiceTest {

    private ValidationService service;
    private AssumptionDTO assumptions;

    @BeforeEach
    void setUp() {
        service = new ValidationService();
        assumptions = new AssumptionDTO();
        assumptions.setBaseSalary(42_000_000L);
        assumptions.setJanuaryMultiplier(new BigDecimal("2.0"));
        assumptions.setLiving(4_000_000L);
        assumptions.setChildThrough2026(1_000_000L);
        assumptions.setChildQ12027(1_000_000L);
        assumptions.setChildFromApr2027(1_000_000L);
        assumptions.setRentThroughJan2027(2_000_000L);
        assumptions.setRentFromFeb2027(3_000_000L);
        assumptions.setStartingSavings(5_000_000L);
        Map<Integer, BigDecimal> raises = new HashMap<>();
        for (int year = 2027; year <= 2030; year++) raises.put(year, new BigDecimal("0.08"));
        assumptions.setRaises(raises);
    }

    @Test
    void acceptsValidAssumptions() {
        assertDoesNotThrow(() -> service.validate(assumptions));
    }

    @Test
    void rejectsNegativeMonetaryValues() {
        assumptions.setLiving(-1L);
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void acceptsZeroForAllNonNegativeMonetaryInputs() {
        assumptions.setBaseSalary(0L);
        assumptions.setLiving(0L);
        assumptions.setChildThrough2026(0L);
        assumptions.setChildQ12027(0L);
        assumptions.setChildFromApr2027(0L);
        assumptions.setRentThroughJan2027(0L);
        assumptions.setRentFromFeb2027(0L);
        assumptions.setStartingSavings(0L);

        assertDoesNotThrow(() -> service.validate(assumptions));
    }

    @Test
    void rejectsMissingRequiredMonetaryValues() {
        assumptions.setBaseSalary(null);
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void rejectsMissingOrNonPositiveJanuaryMultiplier() {
        assumptions.setJanuaryMultiplier(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
        assumptions.setJanuaryMultiplier(null);
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void rejectsJanuaryMultiplierAboveMaximum() {
        assumptions.setJanuaryMultiplier(new BigDecimal("20.01"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void acceptsJanuaryMultiplierAtMaximum() {
        assumptions.setJanuaryMultiplier(new BigDecimal("20"));
        assertDoesNotThrow(() -> service.validate(assumptions));
    }

    @Test
    void acceptsRaiseAtZeroAndOneHundredPercentBoundaries() {
        assumptions.getRaises().put(2027, BigDecimal.ZERO);
        assumptions.getRaises().put(2028, BigDecimal.ONE);
        assertDoesNotThrow(() -> service.validate(assumptions));
    }

    @Test
    void rejectsMissingRaiseYear() {
        assumptions.getRaises().remove(2029);
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void rejectsMissingRaisesMapWithoutThrowingNullPointerException() {
        assumptions.setRaises(null);
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void rejectsRaiseOutsideZeroToOneHundredPercent() {
        assumptions.getRaises().put(2030, new BigDecimal("1.01"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

}
