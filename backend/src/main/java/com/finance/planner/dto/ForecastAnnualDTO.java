package com.finance.planner.dto;

public class ForecastAnnualDTO {

    private int year;
    private boolean partial;
    private long salary;
    private long expenses;
    private long netCashFlow;
    private long endingSavings;
    private boolean provisional;

    public ForecastAnnualDTO() {}

    public ForecastAnnualDTO(int year, boolean partial, long salary, long expenses,
                             long netCashFlow, long endingSavings, boolean provisional) {
        this.year = year;
        this.partial = partial;
        this.salary = salary;
        this.expenses = expenses;
        this.netCashFlow = netCashFlow;
        this.endingSavings = endingSavings;
        this.provisional = provisional;
    }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public boolean isPartial() { return partial; }
    public void setPartial(boolean partial) { this.partial = partial; }
    public long getSalary() { return salary; }
    public void setSalary(long salary) { this.salary = salary; }
    public long getExpenses() { return expenses; }
    public void setExpenses(long expenses) { this.expenses = expenses; }
    public long getNetCashFlow() { return netCashFlow; }
    public void setNetCashFlow(long netCashFlow) { this.netCashFlow = netCashFlow; }
    public long getEndingSavings() { return endingSavings; }
    public void setEndingSavings(long endingSavings) { this.endingSavings = endingSavings; }
    public boolean isProvisional() { return provisional; }
    public void setProvisional(boolean provisional) { this.provisional = provisional; }
}