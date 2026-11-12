package com.finance.planner.service;

import com.finance.planner.dto.AssumptionDTO;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    public void validate(AssumptionDTO a) {
        if (a.getBaseSalary() == null || a.getBaseSalary() < 0)
            throw new IllegalArgumentException("baseSalary phải là số VND nguyên không âm.");
        if (a.getJanuaryMultiplier() == null || a.getJanuaryMultiplier().compareTo(BigDecimal.ZERO) <= 0
                || a.getJanuaryMultiplier().compareTo(new BigDecimal("20")) > 0)
            throw new IllegalArgumentException("Hệ số lương tháng 1 phải lớn hơn 0 và không vượt quá 20.");
        if (a.getLiving() == null || a.getLiving() < 0)
            throw new IllegalArgumentException("living phải là số VND nguyên không âm.");
        if (a.getChildThrough2026() == null || a.getChildThrough2026() < 0)
            throw new IllegalArgumentException("childThrough2026 phải là số VND nguyên không âm.");
        if (a.getChildQ12027() == null || a.getChildQ12027() < 0)
            throw new IllegalArgumentException("childQ12027 phải là số VND nguyên không âm.");
        if (a.getChildFromApr2027() == null || a.getChildFromApr2027() < 0)
            throw new IllegalArgumentException("childFromApr2027 phải là số VND nguyên không âm.");
        if (a.getRentThroughJan2027() == null || a.getRentThroughJan2027() < 0)
            throw new IllegalArgumentException("rentThroughJan2027 phải là số VND nguyên không âm.");
        if (a.getRentFromFeb2027() == null || a.getRentFromFeb2027() < 0)
            throw new IllegalArgumentException("rentFromFeb2027 phải là số VND nguyên không âm.");
        if (a.getLoanPrincipal() == null || a.getLoanPrincipal() < 0)
            throw new IllegalArgumentException("loanPrincipal phải là số VND nguyên không âm.");
        if (a.getFixedPayment() == null || a.getFixedPayment() < 0)
            throw new IllegalArgumentException("fixedPayment phải là số VND nguyên không âm.");
        if (a.getNovInterest() == null || a.getNovInterest() < 0)
            throw new IllegalArgumentException("novInterest phải là số VND nguyên không âm.");
        if (a.getDecInterest() == null || a.getDecInterest() < 0)
            throw new IllegalArgumentException("decInterest phải là số VND nguyên không âm.");
        if (a.getStartingSavings() == null || a.getStartingSavings() < 0)
            throw new IllegalArgumentException("startingSavings phải là số VND nguyên không âm.");
        if (a.getNovInterest() > a.getFixedPayment())
            throw new IllegalArgumentException("Lãi từng kỳ không thể lớn hơn khoản trả cố định của kỳ đó.");
        if (a.getDecInterest() > a.getFixedPayment())
            throw new IllegalArgumentException("Lãi từng kỳ không thể lớn hơn khoản trả cố định của kỳ đó.");
        for (int y : new int[]{2027, 2028, 2029, 2030}) {
            BigDecimal r = a.getRaises().get(y);
            if (r == null || r.compareTo(BigDecimal.ZERO) < 0 || r.compareTo(BigDecimal.ONE) > 0)
                throw new IllegalArgumentException("Mức tăng lương năm " + y + " phải từ 0% đến 100%.");
        }
        if (Boolean.TRUE.equals(a.getPayoffInterestConfirmed()) && a.getPayoffInterest() == null)
            throw new IllegalArgumentException("Nhập số lãi tất toán trước khi đánh dấu đã xác nhận.");
    }
}