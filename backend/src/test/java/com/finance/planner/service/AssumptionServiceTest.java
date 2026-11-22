package com.finance.planner.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.finance.planner.dto.IncomeChangeDTO;
import com.finance.planner.entity.Assumption;
import com.finance.planner.household.*;
import com.finance.planner.repository.AssumptionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssumptionServiceTest {
    @Mock AssumptionRepository repository;
    @Mock ExpenseScheduleService expenseScheduleService;
    @Mock IncomeSchedulePersistenceService incomeSchedulePersistenceService;
    @Mock HouseholdRepository householdRepository;
    @InjectMocks AssumptionService service;

    @Test void getAssumptionsInitializesANewHouseholdWithBlankFinancialInputs() {
        HouseholdContext context = new HouseholdContext(1L, 10L, HouseholdRole.OWNER);
        Household household = new Household("Test", true);
        when(householdRepository.findForUpdateById(1L)).thenReturn(Optional.of(household));
        when(householdRepository.getReferenceById(1L)).thenReturn(household);
        when(repository.findTopByHouseholdIdOrderByIdAsc(1L)).thenReturn(null);
        when(repository.save(any(Assumption.class))).thenAnswer(invocation -> { Assumption a=invocation.getArgument(0); a.setId(5L); return a; });
        when(incomeSchedulePersistenceService.getIncomeChanges(any())).thenReturn(List.of(new IncomeChangeDTO("2026-11",0L)));
        when(expenseScheduleService.getExpenses(context)).thenReturn(List.of());

        var result = service.getAssumptions(context);

        ArgumentCaptor<Assumption> saved = ArgumentCaptor.forClass(Assumption.class);
        verify(repository).save(saved.capture());
        verify(incomeSchedulePersistenceService).saveIncomeChanges(eq(saved.getValue()), anyList());
        verify(expenseScheduleService).saveExpenses(context, List.of());
        assertEquals(household, saved.getValue().getHousehold());
        assertEquals(4, result.getForecastYears());
        assertEquals(0L, result.getIncomeChanges().getFirst().amount());
        assertEquals(0L, result.getStartingSavings());
        assertTrue(result.getExpenses().isEmpty());
        assertNotNull(saved.getValue().getCreatedAt());
    }
}
