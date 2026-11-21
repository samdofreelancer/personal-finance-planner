package com.finance.planner.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class AssumptionDTO {

    private Long baseSalary;
    private BigDecimal januaryMultiplier;
    private Long startingSavings;
    private Map<Integer, BigDecimal> raises = new HashMap<>();
    private List<ExpenseDTO> expenses = new ArrayList<>();

    // Getters & setters
    public Long getBaseSalary() { return baseSalary; }
    public void setBaseSalary(Long baseSalary) { this.baseSalary = baseSalary; }
    public BigDecimal getJanuaryMultiplier() { return januaryMultiplier; }
    public void setJanuaryMultiplier(BigDecimal januaryMultiplier) { this.januaryMultiplier = januaryMultiplier; }
    public Long getStartingSavings() { return startingSavings; }
    public void setStartingSavings(Long startingSavings) { this.startingSavings = startingSavings; }
    public Map<Integer, BigDecimal> getRaises() { return raises; }
    public void setRaises(Map<Integer, BigDecimal> raises) { this.raises = raises; }
    public List<ExpenseDTO> getExpenses() { return expenses; }
    public void setExpenses(List<ExpenseDTO> expenses) { this.expenses = expenses; }
}
