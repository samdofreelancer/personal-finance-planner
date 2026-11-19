package com.finance.planner.dto;

import java.util.List;

public record LoanDTO(Long id, String name, Long openingPrincipal, List<LoanRepaymentDTO> repayments) {}
