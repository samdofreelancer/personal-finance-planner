package com.finance.planner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.planner.dto.ExpenseChangeDTO;
import com.finance.planner.dto.ExpenseDTO;
import com.finance.planner.entity.ExpenseAmountChange;
import com.finance.planner.entity.ExpenseCategory;
import com.finance.planner.repository.ExpenseCategoryRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpenseScheduleServiceTest {

    @Mock private ExpenseCategoryRepository repository;
    @InjectMocks private ExpenseScheduleService service;

    @Test
    void getExpensesMapsPersistedCategoriesAndEffectiveMonths() {
        ExpenseCategory category = category(" Học phí ", "2026-11", 1_500L);
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(category));

        List<ExpenseDTO> result = service.getExpenses();

        assertEquals(1, result.size());
        assertEquals(" Học phí ", result.get(0).getName());
        assertEquals("2026-11", result.get(0).getChanges().get(0).getEffectiveMonth());
        assertEquals(1_500L, result.get(0).getChanges().get(0).getAmount());
    }

    @Test
    void saveExpensesReplacesExistingCategoriesAndNormalizesNames() {
        ExpenseDTO requested = new ExpenseDTO("  Tiền nhà  ", List.of(
                new ExpenseChangeDTO("2026-11", 5_000L), new ExpenseChangeDTO("2027-02", 6_000L)));
        when(repository.findAll()).thenReturn(List.of(new ExpenseCategory()));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ExpenseDTO> result = service.saveExpenses(List.of(requested));

        InOrder order = inOrder(repository);
        order.verify(repository).findAll();
        order.verify(repository).deleteAll(org.mockito.ArgumentMatchers.anyList());
        order.verify(repository).flush();
        ArgumentCaptor<List<ExpenseCategory>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        ExpenseCategory saved = captor.getValue().get(0);
        assertEquals("Tiền nhà", saved.getName());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
        assertEquals(LocalDate.of(2026, 11, 1), saved.getChanges().get(0).getEffectiveMonth());
        assertEquals(LocalDate.of(2027, 2, 1), saved.getChanges().get(1).getEffectiveMonth());
        assertEquals(saved, saved.getChanges().get(0).getCategory());
        assertEquals(1, result.size());
        assertEquals("Tiền nhà", result.get(0).getName());
        assertEquals(requested.getChanges().get(0).getAmount(), result.get(0).getChanges().get(0).getAmount());
    }

    @Test
    void seedDefaultsPersistsTheDefaultExpenseSchedules() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ExpenseDTO> result = service.seedDefaults();

        assertEquals(3, result.size());
        assertEquals("Sinh hoạt phí", result.get(0).getName());
        assertEquals(4_250_000L, result.get(0).getChanges().get(0).getAmount());
        assertEquals("Quỹ nuôi con", result.get(1).getName());
        assertEquals(3, result.get(1).getChanges().size());
        assertEquals("Tiền nhà", result.get(2).getName());
        assertEquals("2027-02", result.get(2).getChanges().get(1).getEffectiveMonth());
    }

    private ExpenseCategory category(String name, String month, long amount) {
        ExpenseCategory category = new ExpenseCategory();
        category.setName(name);
        ExpenseAmountChange change = new ExpenseAmountChange();
        change.setEffectiveMonth(LocalDate.parse(month + "-01"));
        change.setAmount(amount);
        category.addChange(change);
        return category;
    }
}
