package com.finance.planner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.dto.ForecastAnnualDTO;
import com.finance.planner.dto.ForecastMonthDTO;
import com.finance.planner.dto.ForecastResponse;
import com.finance.planner.dto.ExpenseDTO;
import com.finance.planner.dto.ExpenseChangeDTO;
import com.finance.planner.dto.LoanDTO;
import com.finance.planner.dto.LoanRepaymentDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForecastServiceTest {

    private ForecastService service;
    private AssumptionDTO assumptions;
    private List<LoanDTO> loans;

    @BeforeEach
    void setUp() {
        service = new ForecastService();
        assumptions = new AssumptionDTO();
        assumptions.setBaseSalary(1_000L);
        assumptions.setJanuaryMultiplier(new BigDecimal("2.0"));
        assumptions.setExpenses(List.of(
                new ExpenseDTO("Sinh hoạt phí", List.of(new ExpenseChangeDTO("2026-11", 100L))),
                new ExpenseDTO("Nuôi con", List.of(
                        new ExpenseChangeDTO("2026-11", 10L),
                        new ExpenseChangeDTO("2027-01", 20L),
                        new ExpenseChangeDTO("2027-04", 30L))),
                new ExpenseDTO("Tiền nhà", List.of(
                        new ExpenseChangeDTO("2026-11", 40L),
                        new ExpenseChangeDTO("2027-02", 50L)))));
        assumptions.setStartingSavings(500L);
        Map<Integer, BigDecimal> raises = new HashMap<>();
        for (int year = 2027; year <= 2030; year++) raises.put(year, BigDecimal.ZERO);
        assumptions.setRaises(raises);
        loans = List.of(new LoanDTO(1L, "Khoản vay mẫu", 1_000L, List.of(
                new LoanRepaymentDTO(1L, LocalDate.of(2026, 11, 30), 300L, 100L),
                new LoanRepaymentDTO(2L, LocalDate.of(2026, 12, 31), 350L, 50L),
                new LoanRepaymentDTO(3L, LocalDate.of(2027, 1, 31), 350L, 25L))));
    }

    @Test
    void buildsFiftyMonthsAndFiveAnnualSummaries() {
        ForecastResponse result = service.buildForecast(assumptions, loans);

        assertEquals(50, result.getMonths().size());
        assertEquals(5, result.getAnnual().size());
        assertEquals(2026, result.getMonths().get(0).getYear());
        assertEquals(11, result.getMonths().get(0).getMonth());
        assertEquals(2030, result.getMonths().get(49).getYear());
        assertEquals(12, result.getMonths().get(49).getMonth());
        assertTrue(result.getAnnual().get(0).isPartial());
        assertFalse(result.getAnnual().get(1).isPartial());
    }

    @Test
    void appliesNovemberAndDecemberDebtPaymentsAgainstPrincipal() {
        ForecastResponse result = service.buildForecast(assumptions, loans);
        ForecastMonthDTO november = result.getMonths().get(0);
        ForecastMonthDTO december = result.getMonths().get(1);

        assertEquals(100L, november.getInterestPaid());
        assertEquals(300L, november.getPrincipalPaid());
        assertEquals(400L, november.getLoanPayment());
        assertEquals(1_000L, november.getOpeningPrincipal());
        assertEquals(50L, december.getInterestPaid());
        assertEquals(350L, december.getPrincipalPaid());
        assertEquals(700L, december.getOpeningPrincipal());
    }

    @Test
    void capsPrincipalPaymentAtRemainingBalance() {
        List<LoanDTO> capped = List.of(new LoanDTO(1L, "Khoản vay mẫu", 200L, loans.get(0).repayments()));

        ForecastMonthDTO november = service.buildForecast(assumptions, capped).getMonths().get(0);

        assertEquals(200L, november.getPrincipalPaid());
        assertEquals(200L, november.getOpeningPrincipal());
        assertEquals(300L, november.getLoanPayment());
    }

    @Test
    void ignoresRepaymentsOutsideForecastWindowAndKeepsRemainingDebt() {
        List<LoanDTO> extended = List.of(new LoanDTO(1L, "Khoản vay mẫu", 1_000L, List.of(
                new LoanRepaymentDTO(1L, LocalDate.of(2031, 1, 1), 500L, 25L))));

        ForecastMonthDTO finalMonth = service.buildForecast(assumptions, extended).getMonths().get(49);

        assertEquals(1_000L, finalMonth.getOpeningPrincipal());
        assertEquals(0L, finalMonth.getPrincipalPaid());
        assertEquals(0L, finalMonth.getInterestPaid());
        assertEquals(0L, finalMonth.getLoanPayment());
    }

    @Test
    void appliesScheduledPayoffInJanuaryAndKeepsSavingsDeterministic() {
        ForecastMonthDTO january = service.buildForecast(assumptions, loans).getMonths().get(2);

        assertEquals(350L, january.getOpeningPrincipal());
        assertEquals(350L, january.getPrincipalPaid());
        assertEquals(25L, january.getInterestPaid());
        assertEquals(375L, january.getLoanPayment());
        assertEquals(535L, january.getTotalExpenses());
        assertEquals(1_465L, january.getNetCashFlow());
        assertEquals(2_865L, january.getCumulativeSavings());
        assertFalse(january.isProvisional());
        assertFalse(january.isSavingsProvisional());

        ForecastMonthDTO february = service.buildForecast(assumptions, loans).getMonths().get(3);
        assertEquals(3_695L, february.getCumulativeSavings());
        assertFalse(february.isSavingsProvisional());
    }

    @Test
    void zeroInterestIsAnExplicitScheduledAmountAndIsNotProvisional() {
        List<LoanDTO> zeroInterest = List.of(new LoanDTO(1L, "Khoản vay mẫu", 1_000L, List.of(
                new LoanRepaymentDTO(1L, LocalDate.of(2026, 11, 30), 300L, 100L),
                new LoanRepaymentDTO(2L, LocalDate.of(2026, 12, 31), 350L, 50L),
                new LoanRepaymentDTO(3L, LocalDate.of(2027, 1, 31), 350L, 0L))));

        ForecastMonthDTO january = service.buildForecast(assumptions, zeroInterest).getMonths().get(2);

        assertEquals(0L, january.getInterestPaid());
        assertEquals(350L, january.getLoanPayment());
        assertEquals(2_890L, january.getCumulativeSavings());
        assertFalse(january.isProvisional());
    }

    @Test
    void appliesUserDefinedExpenseChangesAtTheirExactMonthBoundaries() {
        var months = service.buildForecast(assumptions, loans).getMonths();

        assertEquals(150L, months.get(0).getRecurringExpenses()); // Nov 2026
        assertEquals(150L, months.get(1).getRecurringExpenses()); // Dec 2026
        assertEquals(160L, months.get(2).getRecurringExpenses()); // Jan 2027
        assertEquals(170L, months.get(4).getRecurringExpenses()); // Mar 2027
        assertEquals(180L, months.get(5).getRecurringExpenses()); // Apr 2027
        assertEquals(3, months.get(5).getExpenseBreakdown().size());
    }

    @Test
    void expensesWithoutAnEffectiveAmountAreZeroUntilTheirFirstEffectiveMonth() {
        assumptions.setExpenses(List.of(new ExpenseDTO("Học phí", List.of(
                new ExpenseChangeDTO("2027-01", 500L)))));

        var months = service.buildForecast(assumptions, loans).getMonths();

        assertEquals(0L, months.get(0).getRecurringExpenses());
        assertEquals(500L, months.get(2).getRecurringExpenses());
    }

    @Test
    void allowsAnExpenseToBeStoppedBySettingItsAmountToZero() {
        assumptions.setExpenses(List.of(new ExpenseDTO("Học phí", List.of(
                new ExpenseChangeDTO("2026-11", 500L),
                new ExpenseChangeDTO("2027-02", 0L)))));

        var months = service.buildForecast(assumptions, loans).getMonths();

        assertEquals(500L, months.get(2).getRecurringExpenses());
        assertEquals(0L, months.get(3).getRecurringExpenses());
    }

    @Test
    void appliesRaisesFromAprilAndJanuaryMultiplierOnlyInJanuary() {
        assumptions.setBaseSalary(1_001L);
        assumptions.setJanuaryMultiplier(new BigDecimal("1.5"));
        assumptions.getRaises().put(2027, new BigDecimal("0.10"));

        var months = service.buildForecast(assumptions, loans).getMonths();

        assertEquals(1_001L, months.get(4).getSalary()); // Mar 2027
        assertEquals(1_101L, months.get(5).getSalary()); // Apr 2027, 10% raise
        assertEquals(1_502L, months.get(2).getSalary()); // Jan 2027, HALF_UP
    }

    @Test
    void calculatesCashFlowAndAnnualTotalsFromMonthlyResults() {
        ForecastResponse result = service.buildForecast(assumptions, loans);
        ForecastMonthDTO november = result.getMonths().get(0);
        ForecastAnnualDTO year2026 = result.getAnnual().get(0);

        assertEquals(1_000L, november.getSalary());
        assertEquals(550L, november.getTotalExpenses());
        assertEquals(450L, november.getNetCashFlow());
        assertEquals(950L, november.getCumulativeSavings());
        assertEquals(2, result.getMonths().stream().filter(m -> m.getYear() == 2026).count());
        assertEquals(2_000L, year2026.getSalary());
        assertEquals(900L, year2026.getNetCashFlow());
        assertEquals(1_400L, year2026.getEndingSavings());
    }

    @Test
    void preservesNegativeSavingsAsShortfall() {
        assumptions.setStartingSavings(0L);
        assumptions.setExpenses(List.of(new ExpenseDTO("Sinh hoạt", List.of(
                new ExpenseChangeDTO("2026-11", 10_000L)))));

        ForecastMonthDTO november = service.buildForecast(assumptions, loans).getMonths().get(0);

        assertTrue(november.getCumulativeSavings() < 0);
        assertEquals(-9_400L, november.getCumulativeSavings());
    }
}
