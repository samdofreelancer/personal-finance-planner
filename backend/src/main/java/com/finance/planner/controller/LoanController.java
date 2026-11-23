package com.finance.planner.controller;

import com.finance.planner.dto.LoanDTO;
import com.finance.planner.service.LoanScheduleService;
import com.finance.planner.security.HouseholdContextResolver;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanScheduleService service;
    private final HouseholdContextResolver contextResolver;

    public LoanController(LoanScheduleService service, HouseholdContextResolver contextResolver) {
        this.service = service;
        this.contextResolver = contextResolver;
    }

    @GetMapping
    public ResponseEntity<List<LoanDTO>> get() {
        return ResponseEntity.ok(service.getLoans(contextResolver.requireContext()));
    }

    @PutMapping
    public ResponseEntity<List<LoanDTO>> put(@RequestBody List<LoanDTO> loans) {
        return ResponseEntity.ok(service.replaceLoans(contextResolver.requireContext(), loans));
    }
}
