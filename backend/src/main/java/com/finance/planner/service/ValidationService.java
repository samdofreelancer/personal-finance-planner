package com.finance.planner.service;

import com.finance.planner.dto.AssumptionDTO;
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
        if (a.getBaseSalary() == null || a.getBaseSalary() < 0)
            throw new IllegalArgumentException("baseSalary phải là số VND nguyên không âm.");
        if (a.getJanuaryMultiplier() == null || a.getJanuaryMultiplier().compareTo(BigDecimal.ZERO) <= 0
                || a.getJanuaryMultiplier().compareTo(new BigDecimal("20")) > 0)
            throw new IllegalArgumentException("Hệ số lương tháng 1 phải lớn hơn 0 và không vượt quá 20.");
        validateExpenses(a);
        if (a.getStartingSavings() == null || a.getStartingSavings() < 0)
            throw new IllegalArgumentException("startingSavings phải là số VND nguyên không âm.");
        if (a.getRaises() == null)
            throw new IllegalArgumentException("Danh sách mức tăng lương không được để trống.");
        for (int y : new int[]{2027, 2028, 2029, 2030}) {
            BigDecimal r = a.getRaises().get(y);
            if (r == null || r.compareTo(BigDecimal.ZERO) < 0 || r.compareTo(BigDecimal.ONE) > 0)
                throw new IllegalArgumentException("Mức tăng lương năm " + y + " phải từ 0% đến 100%.");
        }
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
