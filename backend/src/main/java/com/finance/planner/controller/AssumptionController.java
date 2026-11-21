package com.finance.planner.controller;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.service.AssumptionService;
import com.finance.planner.service.ValidationService;
import com.finance.planner.service.LoanScheduleService;
import com.finance.planner.security.HouseholdContextResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/assumptions")
public class AssumptionController {

    private final AssumptionService assumptionService;
    private final ValidationService validationService;
    private final LoanScheduleService loanScheduleService;
    private final HouseholdContextResolver contextResolver;

    public AssumptionController(AssumptionService assumptionService, ValidationService validationService,
                                LoanScheduleService loanScheduleService, HouseholdContextResolver contextResolver) {
        this.assumptionService = assumptionService;
        this.validationService = validationService;
        this.loanScheduleService = loanScheduleService;
        this.contextResolver = contextResolver;
    }

    @GetMapping
    public ResponseEntity<AssumptionDTO> get() {
        return ResponseEntity.ok(assumptionService.getAssumptions(contextResolver.requireContext()));
    }

    @PutMapping
    public ResponseEntity<AssumptionDTO> put(@RequestBody AssumptionDTO dto) {
        validationService.validate(dto);
        return ResponseEntity.ok(assumptionService.saveAssumptions(contextResolver.requireContext(), dto));
    }

    @PostMapping("/seed")
    @Transactional
    public ResponseEntity<AssumptionDTO> seed() {
        var context = contextResolver.requireContext();
        AssumptionDTO defaults = assumptionService.seedDefaults(context);
        loanScheduleService.resetToDefaultSample(context);
        return ResponseEntity.ok(defaults);
    }
}
