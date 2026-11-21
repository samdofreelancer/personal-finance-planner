package com.finance.planner.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AssumptionDTO {

    private List<IncomeChangeDTO> incomeChanges = new ArrayList<>();
    private BigDecimal januaryMultiplier;
    private Long startingSavings;
    private Integer forecastYears = 4;
    private List<ExpenseDTO> expenses = new ArrayList<>();


    // Getters & setters
    public List<IncomeChangeDTO> getIncomeChanges() { return incomeChanges; }
    public void setIncomeChanges(List<IncomeChangeDTO> incomeChanges) { this.incomeChanges = incomeChanges; }
    public BigDecimal getJanuaryMultiplier() { return januaryMultiplier; }
    public void setJanuaryMultiplier(BigDecimal januaryMultiplier) { this.januaryMultiplier = januaryMultiplier; }
    public Long getStartingSavings() { return startingSavings; }
    public void setStartingSavings(Long startingSavings) { this.startingSavings = startingSavings; }
    public Integer getForecastYears() { return forecastYears; }
    public void setForecastYears(Integer forecastYears) { this.forecastYears = forecastYears; }
    public List<ExpenseDTO> getExpenses() { return expenses; }
    public void setExpenses(List<ExpenseDTO> expenses) { this.expenses = expenses; }
}
