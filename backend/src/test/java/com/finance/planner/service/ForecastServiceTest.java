package com.finance.planner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.dto.ForecastAnnualDTO;
import com.finance.planner.dto.ForecastMonthDTO;
import com.finance.planner.dto.ForecastResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForecastServiceTest {

    private ForecastService service;
    private AssumptionDTO assumptions;

    @BeforeEach
    void setUp() {
        service = new ForecastService();
        assumptions = new AssumptionDTO();
        assumptions.setBaseSalary(1_000L);
        assumptions.setJanuaryMultiplier(new BigDecimal("2.0"));
        assumptions.setLiving(100L);
        assumptions.setChildThrough2026(10L);
        assumptions.setChildQ12027(20L);
        assumptions.setChildFromApr2027(30L);
        assumptions.setRentThroughJan2027(40L);
        assumptions.setRentFromFeb2027(50L);
        assumptions.setLoanPrincipal(1_000L);
        assumptions.setFixedPayment(400L);
        assumptions.setNovInterest(100L);
        assumptions.setDecInterest(50L);
        assumptions.setPayoffInterest(25L);
        assumptions.setPayoffInterestConfirmed(false);
        assumptions.setStartingSavings(500L);
        Map<Integer, BigDecimal> raises = new HashMap<>();
        for (int year = 2027; year <= 2030; year++) raises.put(year, BigDecimal.ZERO);
        assumptions.setRaises(raises);
    }

    @Test
    void buildsFiftyMonthsAndFiveAnnualSummaries() {
        ForecastResponse result = service.buildForecast(assumptions);

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
        ForecastResponse result = service.buildForecast(assumptions);
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
        assumptions.setLoanPrincipal(200L);

        ForecastMonthDTO november = service.buildForecast(assumptions).getMonths().get(0);

        assertEquals(200L, november.getPrincipalPaid());
        assertEquals(200L, november.getOpeningPrincipal());
        assertEquals(300L, november.getLoanPayment());
    }

    @Test
    void paysOffRemainingPrincipalInJanuaryAndMarksUnconfirmedInterestProvisional() {
        ForecastMonthDTO january = service.buildForecast(assumptions).getMonths().get(2);

        assertEquals(350L, january.getOpeningPrincipal());
        assertEquals(350L, january.getPrincipalPaid());
        assertEquals(25L, january.getInterestPaid());
        assertEquals(375L, january.getLoanPayment());
        assertTrue(january.isProvisional());
        assertTrue(january.isSavingsProvisional());
    }

    @Test
    void confirmedPayoffInterestIsNotProvisional() {
        assumptions.setPayoffInterestConfirmed(true);

        ForecastMonthDTO january = service.buildForecast(assumptions).getMonths().get(2);

        assertFalse(january.isProvisional());
        assertFalse(january.isSavingsProvisional());
    }

    @Test
    void missingPayoffInterestIsTreatedAsZeroAndProvisional() {
        assumptions.setPayoffInterest(null);

        ForecastMonthDTO january = service.buildForecast(assumptions).getMonths().get(2);

        assertEquals(0L, january.getInterestPaid());
        assertEquals(350L, january.getLoanPayment());
        assertTrue(january.isProvisional());
    }

    @Test
    void appliesChildAndRentChangesAtTheirExactMonthBoundaries() {
        var months = service.buildForecast(assumptions).getMonths();

        assertEquals(10L, months.get(1).getChild()); // Dec 2026
        assertEquals(20L, months.get(2).getChild()); // Jan 2027
        assertEquals(20L, months.get(4).getChild()); // Mar 2027
        assertEquals(30L, months.get(5).getChild()); // Apr 2027
        assertEquals(40L, months.get(2).getRent()); // Jan 2027
        assertEquals(50L, months.get(3).getRent()); // Feb 2027
    }

    @Test
    void appliesRaisesFromAprilAndJanuaryMultiplierOnlyInJanuary() {
        assumptions.setBaseSalary(1_001L);
        assumptions.setJanuaryMultiplier(new BigDecimal("1.5"));
        assumptions.getRaises().put(2027, new BigDecimal("0.10"));

        var months = service.buildForecast(assumptions).getMonths();

        assertEquals(1_001L, months.get(4).getSalary()); // Mar 2027
        assertEquals(1_101L, months.get(5).getSalary()); // Apr 2027, 10% raise
        assertEquals(1_502L, months.get(2).getSalary()); // Jan 2027, HALF_UP
    }

    @Test
    void calculatesCashFlowAndAnnualTotalsFromMonthlyResults() {
        ForecastResponse result = service.buildForecast(assumptions);
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
        assumptions.setLiving(10_000L);

        ForecastMonthDTO november = service.buildForecast(assumptions).getMonths().get(0);

        assertTrue(november.getCumulativeSavings() < 0);
        assertEquals(-9_450L, november.getCumulativeSavings());
    }
}
