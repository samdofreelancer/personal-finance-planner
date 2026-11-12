package com.finance.planner.dto;

import java.util.List;

public class ForecastResponse {

    private List<ForecastMonthDTO> months;
    private List<ForecastAnnualDTO> annual;

    public ForecastResponse() {}

    public ForecastResponse(List<ForecastMonthDTO> months, List<ForecastAnnualDTO> annual) {
        this.months = months;
        this.annual = annual;
    }

    public List<ForecastMonthDTO> getMonths() { return months; }
    public void setMonths(List<ForecastMonthDTO> months) { this.months = months; }
    public List<ForecastAnnualDTO> getAnnual() { return annual; }
    public void setAnnual(List<ForecastAnnualDTO> annual) { this.annual = annual; }
}