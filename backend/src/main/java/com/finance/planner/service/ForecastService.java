package com.finance.planner.service;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.dto.ForecastAnnualDTO;
import com.finance.planner.dto.ForecastMonthDTO;
import com.finance.planner.dto.ForecastResponse;
import com.finance.planner.dto.LoanDTO;
import com.finance.planner.dto.LoanRepaymentDTO;
import com.finance.planner.dto.ExpenseDTO;
import com.finance.planner.dto.ExpenseChangeDTO;
import com.finance.planner.dto.ExpenseBreakdownDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ForecastService {
    private record ScheduledRepayment(long loanKey, LoanRepaymentDTO repayment) {}

    public ForecastResponse buildForecast(AssumptionDTO a, List<LoanDTO> loans) {
        List<ForecastMonthDTO> months = new ArrayList<>();
        Map<Long, Long> remainingByLoan = new HashMap<>();
        Map<YearMonth, List<ScheduledRepayment>> repaymentsByMonth = new HashMap<>();
        for (int i = 0; i < loans.size(); i++) {
            LoanDTO loan = loans.get(i);
            long key = loan.id() == null ? -(i + 1L) : loan.id();
            remainingByLoan.put(key, loan.openingPrincipal());
            for (LoanRepaymentDTO repayment : loan.repayments()) {
                YearMonth period = YearMonth.from(repayment.dueDate());
                repaymentsByMonth.computeIfAbsent(period, ignored -> new ArrayList<>())
                        .add(new ScheduledRepayment(key, repayment));
            }
        }
        long cumulativeSavings = a.getStartingSavings();

        for (int i = 0; i < 50; i++) {
            java.time.LocalDate date = YearMonth.of(2026, 11).plusMonths(i).atDay(1);
            int year = date.getYear();
            int month = date.getMonthValue();
            YearMonth period = YearMonth.from(date);

            long salary = round(a.getBaseSalary()
                    * applyRaises(a, year, month)
                    * (month == 1 ? a.getJanuaryMultiplier().doubleValue() : 1.0));

            List<ExpenseBreakdownDTO> expenseBreakdown = expenseBreakdown(a, period);
            long recurringExpenses = expenseBreakdown.stream().mapToLong(ExpenseBreakdownDTO::amount).sum();

            long openingPrincipal = remainingByLoan.values().stream().mapToLong(Long::longValue).sum();
            long principalPaid = 0;
            long interestPaid = 0;
            for (ScheduledRepayment scheduled : repaymentsByMonth.getOrDefault(period, List.of())) {
                LoanRepaymentDTO repayment = scheduled.repayment();
                long remaining = remainingByLoan.getOrDefault(scheduled.loanKey(), 0L);
                long applied = Math.min(remaining, repayment.principalAmount());
                remainingByLoan.put(scheduled.loanKey(), remaining - applied);
                principalPaid += applied;
                interestPaid += repayment.interestAmount();
            }

            long loanPayment = principalPaid + interestPaid;
            long totalExpenses = recurringExpenses + loanPayment;
            long netCashFlow = salary - totalExpenses;
            cumulativeSavings += netCashFlow;

            months.add(new ForecastMonthDTO(
                    year, month, "thg " + month + " " + year,
                    salary,
                    openingPrincipal, principalPaid, interestPaid,
                    loanPayment, totalExpenses, netCashFlow, cumulativeSavings, false, false));
            ForecastMonthDTO forecastMonth = months.get(months.size() - 1);
            forecastMonth.setRecurringExpenses(recurringExpenses);
            forecastMonth.setExpenseBreakdown(expenseBreakdown);
        }

        List<ForecastAnnualDTO> annual = new ArrayList<>();
        for (int y : new int[]{2026, 2027, 2028, 2029, 2030}) {
            List<ForecastMonthDTO> ym = months.stream().filter(m -> m.getYear() == y).toList();
            boolean partial = y == 2026;
            long totalSalary = ym.stream().mapToLong(ForecastMonthDTO::getSalary).sum();
            long totalExpenses = ym.stream().mapToLong(ForecastMonthDTO::getTotalExpenses).sum();
            long totalNet = ym.stream().mapToLong(ForecastMonthDTO::getNetCashFlow).sum();
            long endSavings = ym.get(ym.size() - 1).getCumulativeSavings();
            boolean prov = ym.stream().anyMatch(ForecastMonthDTO::isProvisional);
            annual.add(new ForecastAnnualDTO(y, partial, totalSalary, totalExpenses, totalNet, endSavings, prov, false));
        }

        return new ForecastResponse(months, annual);
    }

    private double applyRaises(AssumptionDTO a, int year, int month) {
        double factor = 1.0;
        for (int y : new int[]{2027, 2028, 2029, 2030}) {
            BigDecimal r = a.getRaises().get(y);
            if (year > y || (year == y && month >= 4)) {
                factor *= (1.0 + r.doubleValue());
            }
        }
        return factor;
    }

    private List<ExpenseBreakdownDTO> expenseBreakdown(AssumptionDTO assumptions, YearMonth period) {
        List<ExpenseDTO> expenses = assumptions.getExpenses();
        List<ExpenseBreakdownDTO> result = new ArrayList<>();
        for (ExpenseDTO expense : expenses) {
            long amount = expense.getChanges().stream()
                    .filter(change -> !YearMonth.parse(change.getEffectiveMonth()).isAfter(period))
                    .max(java.util.Comparator.comparing(change -> YearMonth.parse(change.getEffectiveMonth())))
                    .map(ExpenseChangeDTO::getAmount).orElse(0L);
            result.add(new ExpenseBreakdownDTO(expense.getName(), amount));
        }
        return result;
    }

    private long round(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
