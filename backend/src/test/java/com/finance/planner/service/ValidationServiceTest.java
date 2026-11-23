package com.finance.planner.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.dto.ExpenseDTO;
import com.finance.planner.dto.ExpenseChangeDTO;
import com.finance.planner.dto.IncomeChangeDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidationServiceTest {

    private ValidationService service;
    private AssumptionDTO assumptions;

    @BeforeEach
    void setUp() {
        service = new ValidationService();
        assumptions = new AssumptionDTO();
        assumptions.setIncomeChanges(java.util.List.of(new IncomeChangeDTO("2026-11", 42_000_000L)));
        assumptions.setJanuaryMultiplier(new BigDecimal("2.0"));
        assumptions.setExpenses(java.util.List.of(new ExpenseDTO("Sinh hoạt", java.util.List.of(
                new ExpenseChangeDTO("2026-11", 4_000_000L)))));
        assumptions.setStartingSavings(5_000_000L);
    }

    @Test
    void acceptsValidAssumptions() {
        assertDoesNotThrow(() -> service.validate(assumptions));
    }

    @Test
    void acceptsTheMinimumHorizonAndMaximumExpenseCategoryNameLength() {
        assumptions.setForecastYears(1);
        assumptions.setExpenses(java.util.List.of(new ExpenseDTO("x".repeat(120), java.util.List.of(
                new ExpenseChangeDTO("2026-11", 0L)))));

        assertDoesNotThrow(() -> service.validate(assumptions));
    }

    @Test
    void rejectsNegativeMonetaryValues() {
        assumptions.setExpenses(java.util.List.of(new ExpenseDTO("Sinh hoạt", java.util.List.of(
                new ExpenseChangeDTO("2026-11", -1L)))));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void acceptsZeroForAllNonNegativeMonetaryInputs() {
        assumptions.setIncomeChanges(java.util.List.of(new IncomeChangeDTO("2026-11", 0L)));
        assumptions.setExpenses(java.util.List.of(new ExpenseDTO("Sinh hoạt", java.util.List.of(
                new ExpenseChangeDTO("2026-11", 0L)))));
        assumptions.setStartingSavings(0L);

        assertDoesNotThrow(() -> service.validate(assumptions));
    }

    @Test
    void rejectsMissingRequiredMonetaryValues() {
        assumptions.setIncomeChanges(null);
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
    void acceptsIncomeScheduleWithBaselineAndFutureChanges() {
        assumptions.setIncomeChanges(java.util.List.of(
                new IncomeChangeDTO("2026-11", 42_000_000L),
                new IncomeChangeDTO("2027-04", 50_000_000L),
                new IncomeChangeDTO("2032-01", 55_000_000L)));
        assertDoesNotThrow(() -> service.validate(assumptions));
    }

    @Test
    void rejectsMissingNovember2026Baseline() {
        assumptions.setIncomeChanges(java.util.List.of(new IncomeChangeDTO("2026-12", 42_000_000L)));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void rejectsDuplicateOrPreBaselineEffectiveMonths() {
        assumptions.setIncomeChanges(java.util.List.of(
                new IncomeChangeDTO("2026-11", 42_000_000L),
                new IncomeChangeDTO("2026-11", 43_000_000L)));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));

        assumptions.setIncomeChanges(java.util.List.of(
                new IncomeChangeDTO("2026-10", 42_000_000L),
                new IncomeChangeDTO("2026-11", 43_000_000L)));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void rejectsNegativeOrMissingIncomeAmountsAndInvalidMonths() {
        assumptions.setIncomeChanges(java.util.List.of(new IncomeChangeDTO("2026-11", -1L)));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));

        assumptions.setIncomeChanges(java.util.List.of(new IncomeChangeDTO("2026-11", null)));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));

        assumptions.setIncomeChanges(java.util.List.of(new IncomeChangeDTO("2026-13", 42_000_000L)));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void validatesForecastHorizonIndependentlyFromIncomeSchedule() {
        assumptions.setForecastYears(6);
        assertDoesNotThrow(() -> service.validate(assumptions));

        assumptions.setForecastYears(7);
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));

        assumptions.setForecastYears(4);
    }

    @Test
    void acceptsAnEmptyExpenseList() {
        assumptions.setExpenses(java.util.List.of());
        assertDoesNotThrow(() -> service.validate(assumptions));
    }

    @Test
    void rejectsANullExpenseList() {
        assumptions.setExpenses(null);
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void rejectsExpenseWithoutAValidNameOrSchedule() {
        assumptions.setExpenses(java.util.List.of(new ExpenseDTO(" ", java.util.List.of(
                new ExpenseChangeDTO("2026-11", 100L)))));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));

        assumptions.setExpenses(java.util.List.of(new ExpenseDTO("Học phí", java.util.List.of())));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void rejectsInvalidOrDuplicateExpenseEffectiveMonths() {
        assumptions.setExpenses(java.util.List.of(new ExpenseDTO("Học phí", java.util.List.of(
                new ExpenseChangeDTO("2026-13", 100L)))));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));

        assumptions.setExpenses(java.util.List.of(new ExpenseDTO("Học phí", java.util.List.of(
                new ExpenseChangeDTO("2026-11", 100L),
                new ExpenseChangeDTO("2026-11", 200L)))));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

    @Test
    void rejectsDuplicateExpenseNamesIgnoringCase() {
        assumptions.setExpenses(java.util.List.of(
                new ExpenseDTO("Học phí", java.util.List.of(new ExpenseChangeDTO("2026-11", 100L))),
                new ExpenseDTO(" học phí ", java.util.List.of(new ExpenseChangeDTO("2026-11", 200L)))));
        assertThrows(IllegalArgumentException.class, () -> service.validate(assumptions));
    }

}
