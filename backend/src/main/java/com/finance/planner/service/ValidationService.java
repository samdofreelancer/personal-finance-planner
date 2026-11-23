package com.finance.planner.service;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.dto.IncomeChangeDTO;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    public void validate(AssumptionDTO a) {
        validateIncomeChanges(a);
        if (a.getJanuaryMultiplier() == null || a.getJanuaryMultiplier().compareTo(BigDecimal.ZERO) <= 0
                || a.getJanuaryMultiplier().compareTo(new BigDecimal("20")) > 0)
            throw new IllegalArgumentException("Hệ số lương tháng 1 phải lớn hơn 0 và không vượt quá 20.");
        validateExpenses(a);
        if (a.getStartingSavings() == null || a.getStartingSavings() < 0)
            throw new IllegalArgumentException("startingSavings phải là số VND nguyên không âm.");
        if (a.getForecastYears() == null || a.getForecastYears() < 1 || a.getForecastYears() > 6)
            throw new IllegalArgumentException("Thời gian dự phóng phải từ 1 đến 6 năm.");
    }

    private void validateIncomeChanges(AssumptionDTO a) {
        if (a.getIncomeChanges() == null || a.getIncomeChanges().isEmpty())
            throw new IllegalArgumentException("Lịch thu nhập không được để trống.");
        Set<YearMonth> months = new HashSet<>();
        boolean hasBaseline = false;
        for (IncomeChangeDTO change : a.getIncomeChanges()) {
            if (change == null || change.effectiveMonth() == null || change.amount() == null || change.amount() < 0)
                throw new IllegalArgumentException("Mốc thu nhập phải có tháng hiệu lực và số VND nguyên không âm.");
            try {
                YearMonth month = YearMonth.parse(change.effectiveMonth());
                if (month.isBefore(YearMonth.of(2026, 11)))
                    throw new IllegalArgumentException("Tháng hiệu lực thu nhập không được trước 2026-11.");
                if (!months.add(month))
                    throw new IllegalArgumentException("Mỗi tháng chỉ được có một mức thu nhập.");
                if (month.equals(YearMonth.of(2026, 11))) hasBaseline = true;
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Tháng hiệu lực thu nhập phải theo định dạng YYYY-MM.");
            }
        }
        if (!hasBaseline)
            throw new IllegalArgumentException("Lịch thu nhập phải có mốc cơ sở 2026-11.");
    }

    private void validateExpenses(AssumptionDTO a) {
        if (a.getExpenses() == null)
            throw new IllegalArgumentException("Danh sách khoản chi không được để trống.");
        Set<String> names = new HashSet<>();
        for (var expense : a.getExpenses()) {
            if (expense == null || expense.getName() == null || expense.getName().trim().isEmpty()
                    || expense.getName().trim().length() > 120)
                throw new IllegalArgumentException("Tên khoản chi phải có từ 1 đến 120 ký tự.");
            if (!names.add(expense.getName().trim().toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Tên khoản chi không được trùng nhau.");
            if (expense.getChanges() == null || expense.getChanges().isEmpty())
                throw new IllegalArgumentException("Mỗi khoản chi phải có ít nhất một mốc hiệu lực.");
            Set<YearMonth> months = new HashSet<>();
            for (var change : expense.getChanges()) {
                if (change == null || change.getEffectiveMonth() == null || change.getAmount() == null || change.getAmount() < 0)
                    throw new IllegalArgumentException("Mốc chi phí phải có tháng hiệu lực và số VND nguyên không âm.");
                try {
                    YearMonth month = YearMonth.parse(change.getEffectiveMonth());
                    if (!months.add(month)) throw new IllegalArgumentException("Mỗi khoản chi chỉ được có một mức tiền trong cùng tháng.");
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Tháng hiệu lực phải theo định dạng YYYY-MM.");
                }
            }
        }
    }
}
