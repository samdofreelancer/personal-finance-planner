package com.finance.planner.dto;

public class ExpenseChangeDTO {
    private String effectiveMonth;
    private Long amount;

    public ExpenseChangeDTO() {}

    public ExpenseChangeDTO(String effectiveMonth, Long amount) {
        this.effectiveMonth = effectiveMonth;
        this.amount = amount;
    }

    public String getEffectiveMonth() { return effectiveMonth; }
    public void setEffectiveMonth(String effectiveMonth) { this.effectiveMonth = effectiveMonth; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
}
