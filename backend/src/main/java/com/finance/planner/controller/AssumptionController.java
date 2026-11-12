package com.finance.planner.controller;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.service.AssumptionService;
import com.finance.planner.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assumptions")
public class AssumptionController {

    private final AssumptionService assumptionService;
    private final ValidationService validationService;

    public AssumptionController(AssumptionService assumptionService, ValidationService validationService) {
        this.assumptionService = assumptionService;
        this.validationService = validationService;
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
    public ResponseEntity<AssumptionDTO> seed() {
        return ResponseEntity.ok(assumptionService.seedDefaults());
    }
}