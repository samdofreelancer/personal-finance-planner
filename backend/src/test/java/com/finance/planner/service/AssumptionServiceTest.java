package com.finance.planner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.planner.entity.Assumption;
import com.finance.planner.repository.AssumptionRepository;
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

    @InjectMocks
    private AssumptionService service;

    @Test
    void getAssumptionsSeedsDefaultsIntoDatabaseWhenNoRecordExists() {
        when(repository.findTopByOrderByIdAsc()).thenReturn(null);
        when(repository.save(org.mockito.ArgumentMatchers.any(Assumption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.getAssumptions();

        ArgumentCaptor<Assumption> saved = ArgumentCaptor.forClass(Assumption.class);
        verify(repository).save(saved.capture());
        assertEquals(42_000_000L, saved.getValue().getBaseSalary());
        assertNotNull(saved.getValue().getCreatedAt());
        assertNotNull(saved.getValue().getUpdatedAt());
        assertEquals(42_000_000L, result.getBaseSalary());
    }
}
