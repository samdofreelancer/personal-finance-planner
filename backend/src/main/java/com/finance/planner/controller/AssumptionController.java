package com.finance.planner.controller;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.service.AssumptionService;
import com.finance.planner.service.ValidationService;
import com.finance.planner.service.LoanScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/assumptions")
public class AssumptionController {

    private final AssumptionService assumptionService;
    private final ValidationService validationService;
    private final LoanScheduleService loanScheduleService;

    public AssumptionController(AssumptionService assumptionService, ValidationService validationService,
                                LoanScheduleService loanScheduleService) {
        this.assumptionService = assumptionService;
        this.validationService = validationService;
        this.loanScheduleService = loanScheduleService;
    }

    @GetMapping
    public ResponseEntity<AssumptionDTO> get() {
        return ResponseEntity.ok(assumptionService.getAssumptions());
    }

    @PutMapping
    public ResponseEntity<AssumptionDTO> put(@RequestBody AssumptionDTO dto) {
        validationService.validate(dto);
        return ResponseEntity.ok(assumptionService.saveAssumptions(dto));
    }

    @PostMapping("/seed")
    @Transactional
    public ResponseEntity<AssumptionDTO> seed() {
        AssumptionDTO defaults = assumptionService.seedDefaults();
        loanScheduleService.resetToDefaultSample();
        return ResponseEntity.ok(defaults);
    }
}
