package com.finance.planner.service;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.entity.Assumption;
import com.finance.planner.repository.AssumptionRepository;
import com.finance.planner.dto.ExpenseDTO;
import com.finance.planner.dto.IncomeChangeDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssumptionService {

    private final AssumptionRepository repository;
    private final ExpenseScheduleService expenseScheduleService;
    private final IncomeSchedulePersistenceService incomeSchedulePersistenceService;

    public AssumptionService(AssumptionRepository repository, ExpenseScheduleService expenseScheduleService,
                             IncomeSchedulePersistenceService incomeSchedulePersistenceService) {
        this.repository = repository;
        this.expenseScheduleService = expenseScheduleService;
        this.incomeSchedulePersistenceService = incomeSchedulePersistenceService;
    }

    public AssumptionDTO getAssumptions() {
        Assumption entity = repository.findTopByOrderByIdAsc();
        if (entity == null) {
            entity = new Assumption();
            updateEntityFromDTO(entity, defaultAssumptions());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity = repository.save(entity);
            incomeSchedulePersistenceService.saveIncomeChanges(entity, defaultAssumptions().getIncomeChanges());
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
        incomeSchedulePersistenceService.saveIncomeChanges(saved, dto.getIncomeChanges());
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
        incomeSchedulePersistenceService.saveIncomeChanges(saved, defaultAssumptions().getIncomeChanges());
        expenseScheduleService.seedDefaults();
        return toDTO(saved);
    }

    private AssumptionDTO defaultAssumptions() {
        AssumptionDTO dto = new AssumptionDTO();
        dto.setIncomeChanges(List.of(
                new IncomeChangeDTO("2026-11", 42_000_000L),
                new IncomeChangeDTO("2027-04", 45_360_000L),
                new IncomeChangeDTO("2028-04", 48_988_800L),
                new IncomeChangeDTO("2029-04", 52_907_904L),
                new IncomeChangeDTO("2030-04", 57_140_536L),
                new IncomeChangeDTO("2031-04", 57_140_536L),
                new IncomeChangeDTO("2032-04", 57_140_536L)));
        dto.setJanuaryMultiplier(new BigDecimal("2.0"));
        dto.setExpenses(ExpenseScheduleService.defaultExpenses());
        dto.setStartingSavings(30_000_000L);
        dto.setForecastYears(4);
        return dto;
    }

    private AssumptionDTO toDTO(Assumption e) {
        AssumptionDTO dto = new AssumptionDTO();
        dto.setIncomeChanges(incomeSchedulePersistenceService.getIncomeChanges(e));
        dto.setJanuaryMultiplier(e.getJanuaryMultiplier());
        List<ExpenseDTO> expenses = expenseScheduleService.getExpenses();
        dto.setExpenses(expenses);
        dto.setStartingSavings(e.getStartingSavings());
        dto.setForecastYears(e.getForecastYears());
        return dto;
    }

    private void updateEntityFromDTO(Assumption e, AssumptionDTO dto) {
        e.setJanuaryMultiplier(dto.getJanuaryMultiplier());
        e.setStartingSavings(dto.getStartingSavings());
        e.setForecastYears(dto.getForecastYears());
    }

}
