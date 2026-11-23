package com.finance.planner.service;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.dto.ExpenseDTO;
import com.finance.planner.dto.IncomeChangeDTO;
import com.finance.planner.entity.Assumption;
import com.finance.planner.household.HouseholdContext;
import com.finance.planner.household.HouseholdRepository;
import com.finance.planner.repository.AssumptionRepository;
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
    private final HouseholdRepository householdRepository;

    public AssumptionService(AssumptionRepository repository, ExpenseScheduleService expenseScheduleService,
            IncomeSchedulePersistenceService incomeSchedulePersistenceService, HouseholdRepository householdRepository) {
        this.repository = repository;
        this.expenseScheduleService = expenseScheduleService;
        this.incomeSchedulePersistenceService = incomeSchedulePersistenceService;
        this.householdRepository = householdRepository;
    }

    public AssumptionDTO getAssumptions(HouseholdContext context) {
        lockHousehold(context);
        Assumption entity = repository.findTopByHouseholdIdOrderByIdAsc(context.householdId());
        if (entity == null) {
            entity = new Assumption();
            entity.setHousehold(householdRepository.getReferenceById(context.householdId()));
            AssumptionDTO initial = emptyHouseholdAssumptions();
            updateEntityFromDTO(entity, initial);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity = repository.save(entity);
            incomeSchedulePersistenceService.saveIncomeChanges(entity, initial.getIncomeChanges());
            expenseScheduleService.saveExpenses(context, initial.getExpenses());
        }
        return toDTO(entity, context);
    }

    public AssumptionDTO saveAssumptions(HouseholdContext context, AssumptionDTO dto) {
        lockHousehold(context);
        Assumption entity = repository.findTopByHouseholdIdOrderByIdAsc(context.householdId());
        if (entity == null) {
            entity = new Assumption();
            entity.setHousehold(householdRepository.getReferenceById(context.householdId()));
        }
        updateEntityFromDTO(entity, dto);
        entity.setUpdatedAt(LocalDateTime.now());
        if (entity.getCreatedAt() == null) entity.setCreatedAt(LocalDateTime.now());
        Assumption saved = repository.save(entity);
        incomeSchedulePersistenceService.saveIncomeChanges(saved, dto.getIncomeChanges());
        expenseScheduleService.saveExpenses(context, dto.getExpenses());
        return toDTO(saved, context);
    }

    public AssumptionDTO seedDefaults(HouseholdContext context) {
        lockHousehold(context);
        Assumption entity = repository.findTopByHouseholdIdOrderByIdAsc(context.householdId());
        if (entity == null) {
            entity = new Assumption();
            entity.setHousehold(householdRepository.getReferenceById(context.householdId()));
            entity.setCreatedAt(LocalDateTime.now());
        }
        AssumptionDTO defaults = defaultAssumptions();
        updateEntityFromDTO(entity, defaults);
        entity.setUpdatedAt(LocalDateTime.now());
        Assumption saved = repository.save(entity);
        incomeSchedulePersistenceService.saveIncomeChanges(saved, defaults.getIncomeChanges());
        expenseScheduleService.seedDefaults(context);
        return toDTO(saved, context);
    }

    private AssumptionDTO defaultAssumptions() {
        AssumptionDTO dto = new AssumptionDTO();
        dto.setIncomeChanges(List.of(
                new IncomeChangeDTO("2026-11", 42_000_000L), new IncomeChangeDTO("2027-04", 45_360_000L),
                new IncomeChangeDTO("2028-04", 48_988_800L), new IncomeChangeDTO("2029-04", 52_907_904L),
                new IncomeChangeDTO("2030-04", 57_140_536L), new IncomeChangeDTO("2031-04", 57_140_536L),
                new IncomeChangeDTO("2032-04", 57_140_536L)));
        dto.setJanuaryMultiplier(new BigDecimal("2.0"));
        dto.setExpenses(ExpenseScheduleService.defaultExpenses());
        dto.setStartingSavings(30_000_000L);
        dto.setForecastYears(4);
        return dto;
    }

    private AssumptionDTO emptyHouseholdAssumptions() {
        AssumptionDTO dto = new AssumptionDTO();
        dto.setIncomeChanges(List.of(new IncomeChangeDTO("2026-11", 0L)));
        dto.setJanuaryMultiplier(new BigDecimal("2.0"));
        dto.setExpenses(List.of());
        dto.setStartingSavings(0L);
        dto.setForecastYears(4);
        return dto;
    }

    private AssumptionDTO toDTO(Assumption entity, HouseholdContext context) {
        AssumptionDTO dto = new AssumptionDTO();
        dto.setIncomeChanges(incomeSchedulePersistenceService.getIncomeChanges(entity));
        dto.setJanuaryMultiplier(entity.getJanuaryMultiplier());
        List<ExpenseDTO> expenses = expenseScheduleService.getExpenses(context);
        dto.setExpenses(expenses);
        dto.setStartingSavings(entity.getStartingSavings());
        dto.setForecastYears(entity.getForecastYears());
        return dto;
    }

    private void updateEntityFromDTO(Assumption entity, AssumptionDTO dto) {
        entity.setJanuaryMultiplier(dto.getJanuaryMultiplier());
        entity.setStartingSavings(dto.getStartingSavings());
        entity.setForecastYears(dto.getForecastYears());
    }

    private void lockHousehold(HouseholdContext context) {
        if (context == null || context.householdId() == null) throw new IllegalArgumentException("Household context is required");
        householdRepository.findForUpdateById(context.householdId())
                .orElseThrow(() -> new IllegalArgumentException("Household does not exist"));
    }
}
