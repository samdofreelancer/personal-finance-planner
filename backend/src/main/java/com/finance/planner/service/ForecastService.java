package com.finance.planner.service;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.dto.ForecastAnnualDTO;
import com.finance.planner.dto.ForecastMonthDTO;
import com.finance.planner.dto.ForecastResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ForecastService {

    public ForecastResponse buildForecast(AssumptionDTO a) {
        List<ForecastMonthDTO> months = new ArrayList<>();
        long openingPrincipal = a.getLoanPrincipal();
        long cumulativeSavings = a.getStartingSavings();
        boolean savingsProvisional = false;

        for (int i = 0; i < 50; i++) {
            java.time.LocalDate date = YearMonth.of(2026, 11).plusMonths(i).atDay(1);
            int year = date.getYear();
            int month = date.getMonthValue();

            long salary = round(a.getBaseSalary()
                    * applyRaises(a, year, month)
                    * (month == 1 ? a.getJanuaryMultiplier().doubleValue() : 1.0));

            long child = childAmount(a, year, month);
            long rent = rentAmount(a, year, month);

            long principalPaid = 0;
            long interestPaid = 0;
            long loanPayment = 0;
            boolean provisional = false;

            if (year == 2026 && month == 11) {
                interestPaid = a.getNovInterest();
                principalPaid = Math.min(openingPrincipal, Math.max(0, a.getFixedPayment() - interestPaid));
                loanPayment = principalPaid + interestPaid;
            } else if (year == 2026 && month == 12) {
                interestPaid = a.getDecInterest();
                principalPaid = Math.min(openingPrincipal, Math.max(0, a.getFixedPayment() - interestPaid));
                loanPayment = principalPaid + interestPaid;
            } else if (year == 2027 && month == 1) {
                principalPaid = openingPrincipal;
                if (a.getPayoffInterest() == null || !Boolean.TRUE.equals(a.getPayoffInterestConfirmed())) {
                    provisional = true;
                }
                interestPaid = a.getPayoffInterest() == null ? 0 : a.getPayoffInterest();
                loanPayment = principalPaid + interestPaid;
            }

            if (provisional) savingsProvisional = true;

            openingPrincipal = Math.max(0, openingPrincipal - principalPaid);
            long totalExpenses = a.getLiving() + child + rent + loanPayment;
            long netCashFlow = salary - totalExpenses;
            cumulativeSavings += netCashFlow;

            months.add(new ForecastMonthDTO(
                    year, month, "thg " + month + " " + year,
                    salary, a.getLiving(), child, rent,
                    openingPrincipal + principalPaid, principalPaid, interestPaid,
                    loanPayment, totalExpenses, netCashFlow, cumulativeSavings, provisional, savingsProvisional));
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
            boolean endingSavingsProvisional = ym.get(ym.size() - 1).isSavingsProvisional();
            annual.add(new ForecastAnnualDTO(y, partial, totalSalary, totalExpenses, totalNet, endSavings, prov, endingSavingsProvisional));
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

    private long childAmount(AssumptionDTO a, int year, int month) {
        if (year == 2026) return a.getChildThrough2026();
        if (year == 2027 && month <= 3) return a.getChildQ12027();
        return a.getChildFromApr2027();
    }

    private long rentAmount(AssumptionDTO a, int year, int month) {
        if (year < 2027 || (year == 2027 && month == 1)) return a.getRentThroughJan2027();
        return a.getRentFromFeb2027();
    }

    private long round(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}