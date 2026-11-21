package com.finance.planner.service;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.entity.Assumption;
import com.finance.planner.repository.AssumptionRepository;
import com.finance.planner.dto.ExpenseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssumptionService {

    private final AssumptionRepository repository;
    private final ExpenseScheduleService expenseScheduleService;

    public AssumptionService(AssumptionRepository repository, ExpenseScheduleService expenseScheduleService) {
        this.repository = repository;
        this.expenseScheduleService = expenseScheduleService;
    }

    public AssumptionDTO getAssumptions() {
        Assumption entity = repository.findTopByOrderByIdAsc();
        if (entity == null) {
            entity = new Assumption();
            updateEntityFromDTO(entity, defaultAssumptions());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity = repository.save(entity);
            expenseScheduleService.seedDefaults();
        }
        return toDTO(entity);
    }

    public AssumptionDTO saveAssumptions(AssumptionDTO dto) {
        Assumption entity = repository.findTopByOrderByIdAsc();
        if (entity == null) entity = new Assumption();
        updateEntityFromDTO(entity, dto);
        entity.setUpdatedAt(LocalDateTime.now());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        Assumption saved = repository.save(entity);
        expenseScheduleService.saveExpenses(dto.getExpenses());
        return toDTO(saved);
    }

    public AssumptionDTO seedDefaults() {
        Assumption entity = repository.findTopByOrderByIdAsc();
        if (entity == null) {
            entity = new Assumption();
            entity.setCreatedAt(LocalDateTime.now());
        }
        updateEntityFromDTO(entity, defaultAssumptions());
        entity.setUpdatedAt(LocalDateTime.now());
        Assumption saved = repository.save(entity);
        expenseScheduleService.seedDefaults();
        return toDTO(saved);
    }

    private AssumptionDTO defaultAssumptions() {
        AssumptionDTO dto = new AssumptionDTO();
        dto.setBaseSalary(42_000_000L);
        dto.setJanuaryMultiplier(new BigDecimal("2.0"));
        dto.setExpenses(ExpenseScheduleService.defaultExpenses());
        dto.setStartingSavings(30_000_000L);
        Map<Integer, BigDecimal> raises = new HashMap<>();
        raises.put(2027, new BigDecimal("0.08"));
        raises.put(2028, new BigDecimal("0.08"));
        raises.put(2029, new BigDecimal("0.08"));
        raises.put(2030, new BigDecimal("0.08"));
        dto.setRaises(raises);
        return dto;
    }

    private AssumptionDTO toDTO(Assumption e) {
        AssumptionDTO dto = new AssumptionDTO();
        dto.setBaseSalary(e.getBaseSalary());
        dto.setJanuaryMultiplier(e.getJanuaryMultiplier());
        List<ExpenseDTO> expenses = expenseScheduleService.getExpenses();
        dto.setExpenses(expenses);
        dto.setStartingSavings(e.getStartingSavings());
        Map<Integer, BigDecimal> raises = new HashMap<>();
        raises.put(2027, e.getRaise2027());
        raises.put(2028, e.getRaise2028());
        raises.put(2029, e.getRaise2029());
        raises.put(2030, e.getRaise2030());
        dto.setRaises(raises);
        return dto;
    }

    private void updateEntityFromDTO(Assumption e, AssumptionDTO dto) {
        e.setBaseSalary(dto.getBaseSalary());
        e.setJanuaryMultiplier(dto.getJanuaryMultiplier());
        e.setStartingSavings(dto.getStartingSavings());
        if (dto.getRaises() != null) {
            e.setRaise2027(dto.getRaises().get(2027));
            e.setRaise2028(dto.getRaises().get(2028));
            e.setRaise2029(dto.getRaises().get(2029));
            e.setRaise2030(dto.getRaises().get(2030));
        }
    }

}
