package com.finance.planner.dto;

import java.time.LocalDate;

public record LoanRepaymentDTO(Long id, LocalDate dueDate, Long principalAmount, Long interestAmount) {}
