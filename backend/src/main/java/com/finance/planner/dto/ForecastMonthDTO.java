package com.finance.planner.dto;

import java.time.LocalDate;

public class ForecastMonthDTO {

    private int year;
    private int month;
    private String label;
    private long salary;
    private long living;
    private long child;
    private long rent;
    private long openingPrincipal;
    private long principalPaid;
    private long interestPaid;
    private long loanPayment;
    private long totalExpenses;
    private long netCashFlow;
    private long cumulativeSavings;
    private boolean provisional;
    private boolean savingsProvisional;

    public ForecastMonthDTO() {}

    public ForecastMonthDTO(int year, int month, String label, long salary, long living, long child,
                            long rent, long openingPrincipal, long principalPaid, long interestPaid,
                            long loanPayment, long totalExpenses, long netCashFlow,
                            long cumulativeSavings, boolean provisional, boolean savingsProvisional) {
        this.year = year;
        this.month = month;
        this.label = label;
        this.salary = salary;
        this.living = living;
        this.child = child;
        this.rent = rent;
        this.openingPrincipal = openingPrincipal;
        this.principalPaid = principalPaid;
        this.interestPaid = interestPaid;
        this.loanPayment = loanPayment;
        this.totalExpenses = totalExpenses;
        this.netCashFlow = netCashFlow;
        this.cumulativeSavings = cumulativeSavings;
        this.provisional = provisional;
        this.savingsProvisional = savingsProvisional;
    }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public long getSalary() { return salary; }
    public void setSalary(long salary) { this.salary = salary; }
    public long getLiving() { return living; }
    public void setLiving(long living) { this.living = living; }
    public long getChild() { return child; }
    public void setChild(long child) { this.child = child; }
    public long getRent() { return rent; }
    public void setRent(long rent) { this.rent = rent; }
    public long getOpeningPrincipal() { return openingPrincipal; }
    public void setOpeningPrincipal(long openingPrincipal) { this.openingPrincipal = openingPrincipal; }
    public long getPrincipalPaid() { return principalPaid; }
    public void setPrincipalPaid(long principalPaid) { this.principalPaid = principalPaid; }
    public long getInterestPaid() { return interestPaid; }
    public void setInterestPaid(long interestPaid) { this.interestPaid = interestPaid; }
    public long getLoanPayment() { return loanPayment; }
    public void setLoanPayment(long loanPayment) { this.loanPayment = loanPayment; }
    public long getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(long totalExpenses) { this.totalExpenses = totalExpenses; }
    public long getNetCashFlow() { return netCashFlow; }
    public void setNetCashFlow(long netCashFlow) { this.netCashFlow = netCashFlow; }
    public long getCumulativeSavings() { return cumulativeSavings; }
    public void setCumulativeSavings(long cumulativeSavings) { this.cumulativeSavings = cumulativeSavings; }
    public boolean isProvisional() { return provisional; }
    public void setProvisional(boolean provisional) { this.provisional = provisional; }
    public boolean isSavingsProvisional() { return savingsProvisional; }
    public void setSavingsProvisional(boolean savingsProvisional) { this.savingsProvisional = savingsProvisional; }
}