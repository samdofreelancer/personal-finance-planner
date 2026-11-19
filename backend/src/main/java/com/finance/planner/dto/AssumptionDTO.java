package com.finance.planner.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class AssumptionDTO {

    private Long baseSalary;
    private BigDecimal januaryMultiplier;
    private Long living;
    private Long childThrough2026;
    private Long childQ12027;
    private Long childFromApr2027;
    private Long rentThroughJan2027;
    private Long rentFromFeb2027;
    private Long startingSavings;
    private Map<Integer, BigDecimal> raises = new HashMap<>();

    // Getters & setters
    public Long getBaseSalary() { return baseSalary; }
    public void setBaseSalary(Long baseSalary) { this.baseSalary = baseSalary; }
    public BigDecimal getJanuaryMultiplier() { return januaryMultiplier; }
    public void setJanuaryMultiplier(BigDecimal januaryMultiplier) { this.januaryMultiplier = januaryMultiplier; }
    public Long getLiving() { return living; }
    public void setLiving(Long living) { this.living = living; }
    public Long getChildThrough2026() { return childThrough2026; }
    public void setChildThrough2026(Long childThrough2026) { this.childThrough2026 = childThrough2026; }
    public Long getChildQ12027() { return childQ12027; }
    public void setChildQ12027(Long childQ12027) { this.childQ12027 = childQ12027; }
    public Long getChildFromApr2027() { return childFromApr2027; }
    public void setChildFromApr2027(Long childFromApr2027) { this.childFromApr2027 = childFromApr2027; }
    public Long getRentThroughJan2027() { return rentThroughJan2027; }
    public void setRentThroughJan2027(Long rentThroughJan2027) { this.rentThroughJan2027 = rentThroughJan2027; }
    public Long getRentFromFeb2027() { return rentFromFeb2027; }
    public void setRentFromFeb2027(Long rentFromFeb2027) { this.rentFromFeb2027 = rentFromFeb2027; }
    public Long getStartingSavings() { return startingSavings; }
    public void setStartingSavings(Long startingSavings) { this.startingSavings = startingSavings; }
    public Map<Integer, BigDecimal> getRaises() { return raises; }
    public void setRaises(Map<Integer, BigDecimal> raises) { this.raises = raises; }
}
