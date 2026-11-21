package com.finance.planner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.planner.entity.Assumption;
import com.finance.planner.repository.AssumptionRepository;
import com.finance.planner.dto.IncomeChangeDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssumptionServiceTest {

    @Mock
    private AssumptionRepository repository;

    @Mock
    private ExpenseScheduleService expenseScheduleService;

    @Mock
    private IncomeSchedulePersistenceService incomeSchedulePersistenceService;

    @InjectMocks
    private AssumptionService service;

    @Test
    void getAssumptionsSeedsDefaultsIntoDatabaseWhenNoRecordExists() {
        when(repository.findTopByOrderByIdAsc()).thenReturn(null);
        when(repository.save(org.mockito.ArgumentMatchers.any(Assumption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseScheduleService.getExpenses()).thenReturn(ExpenseScheduleService.defaultExpenses());
        when(incomeSchedulePersistenceService.getIncomeChanges(org.mockito.ArgumentMatchers.any(Assumption.class)))
                .thenReturn(java.util.List.of(
                        new IncomeChangeDTO("2026-11", 42_000_000L),
                        new IncomeChangeDTO("2027-04", 45_360_000L),
                        new IncomeChangeDTO("2028-04", 48_988_800L),
                        new IncomeChangeDTO("2029-04", 52_907_904L),
                        new IncomeChangeDTO("2030-04", 57_140_536L),
                        new IncomeChangeDTO("2031-04", 57_140_536L),
                        new IncomeChangeDTO("2032-04", 57_140_536L)));

        var result = service.getAssumptions();

        ArgumentCaptor<Assumption> saved = ArgumentCaptor.forClass(Assumption.class);
        verify(repository).save(saved.capture());
        verify(expenseScheduleService).seedDefaults();
        assertNotNull(saved.getValue().getCreatedAt());
        assertNotNull(saved.getValue().getUpdatedAt());
        verify(incomeSchedulePersistenceService).saveIncomeChanges(saved.getValue(),
                java.util.List.of(
                        new IncomeChangeDTO("2026-11", 42_000_000L),
                        new IncomeChangeDTO("2027-04", 45_360_000L),
                        new IncomeChangeDTO("2028-04", 48_988_800L),
                        new IncomeChangeDTO("2029-04", 52_907_904L),
                        new IncomeChangeDTO("2030-04", 57_140_536L),
                        new IncomeChangeDTO("2031-04", 57_140_536L),
                        new IncomeChangeDTO("2032-04", 57_140_536L)));
        assertEquals(42_000_000L, result.getIncomeChanges().getFirst().amount());
    }
}
