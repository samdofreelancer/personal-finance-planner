package com.finance.planner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.finance.planner.dto.LoanDTO;
import com.finance.planner.dto.LoanRepaymentDTO;
import com.finance.planner.entity.Loan;
import com.finance.planner.entity.LoanRepayment;
import com.finance.planner.repository.LoanRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanScheduleServiceTest {

    @Mock private LoanRepository repository;
    @InjectMocks private LoanScheduleService service;

    @Test
    void getLoansMapsLoansAndSortsRepaymentsByDueDate() {
        Loan loan = loan(1L, "Khoản vay", 1_000L);
        loan.addRepayment(repayment(2L, "2027-01-01", 200L, 20L));
        loan.addRepayment(repayment(1L, "2026-11-01", 100L, 10L));
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(loan));

        List<LoanDTO> result = service.getLoans();

        assertEquals(1, result.size());
        assertEquals(List.of(1L, 2L), result.get(0).repayments().stream().map(LoanRepaymentDTO::id).toList());
    }

    @Test
    void replaceLoansCreatesLoansAndRepaymentsAndDeletesOmittedLoans() {
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(loan(9L, "Old", 500L)));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        LoanDTO requested = new LoanDTO(null, "  New loan  ", 2_000L,
                List.of(new LoanRepaymentDTO(null, LocalDate.of(2026, 11, 30), 300L, 30L)));

        List<LoanDTO> result = service.replaceLoans(List.of(requested));

        ArgumentCaptor<List<Loan>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).deleteAll(org.mockito.ArgumentMatchers.any(Iterable.class));
        verify(repository).saveAll(captor.capture());
        Loan saved = captor.getValue().get(0);
        assertEquals("New loan", saved.getName());
        assertEquals(2_000L, saved.getOpeningPrincipal());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(saved, saved.getRepayments().get(0).getLoan());
        assertEquals(300L, result.get(0).repayments().get(0).principalAmount());
    }

    @Test
    void replaceLoansUpdatesExistingLoanAndRepaymentAndRemovesOmittedRepayments() {
        Loan existing = loan(1L, "Old", 1_000L);
        existing.setCreatedAt(java.time.LocalDateTime.now().minusDays(1));
        LoanRepayment keep = repayment(10L, "2026-11-01", 100L, 10L);
        LoanRepayment remove = repayment(11L, "2026-12-01", 200L, 20L);
        existing.addRepayment(keep);
        existing.addRepayment(remove);
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(existing));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.replaceLoans(List.of(new LoanDTO(1L, "Updated", 900L,
                List.of(new LoanRepaymentDTO(10L, LocalDate.of(2027, 1, 1), 250L, 25L)))));

        assertEquals("Updated", existing.getName());
        assertEquals(900L, existing.getOpeningPrincipal());
        assertEquals(List.of(keep), existing.getRepayments());
        assertEquals(LocalDate.of(2027, 1, 1), keep.getDueDate());
        assertEquals(250L, keep.getPrincipalAmount());
    }

    @Test
    void replaceLoansRejectsNullListInvalidLoanDuplicateAndUnknownIds() {
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(null));
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(loan(1L, "Existing", 100L)));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(java.util.Arrays.asList((LoanDTO) null)));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, " ", 100L, List.of()))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, "x".repeat(121), 100L, List.of()))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, "Valid", null, List.of()))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, "Valid", -1L, List.of()))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(
                new LoanDTO(1L, "A", 100L, List.of()), new LoanDTO(1L, "B", 100L, List.of()))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(99L, "Missing", 100L, List.of()))));
    }

    @Test
    void replaceLoansRejectsInvalidRepaymentListsEntriesAndIds() {
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(loan(1L, "Existing", 100L)));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, "A", 100L, null))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, "A", 100L,
                java.util.Arrays.asList((LoanRepaymentDTO) null)))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, "A", 100L,
                List.of(new LoanRepaymentDTO(null, null, 1L, 1L))))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, "A", 100L,
                List.of(new LoanRepaymentDTO(null, LocalDate.now(), null, 1L))))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, "A", 100L,
                List.of(new LoanRepaymentDTO(null, LocalDate.now(), -1L, 1L))))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, "A", 100L,
                List.of(new LoanRepaymentDTO(null, LocalDate.now(), 1L, null))))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(null, "A", 100L,
                List.of(new LoanRepaymentDTO(null, LocalDate.now(), 1L, -1L))))));

        Loan existing = loan(1L, "Existing", 100L);
        existing.addRepayment(repayment(10L, "2026-11-01", 1L, 1L));
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(existing));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(1L, "A", 100L,
                List.of(new LoanRepaymentDTO(10L, LocalDate.now(), 1L, 1L),
                        new LoanRepaymentDTO(10L, LocalDate.now(), 1L, 1L))))));
        assertThrows(IllegalArgumentException.class, () -> service.replaceLoans(List.of(new LoanDTO(1L, "A", 100L,
                List.of(new LoanRepaymentDTO(99L, LocalDate.now(), 1L, 1L))))));
    }

    @Test
    void resetToSampleReplacesExistingLoansAndClearsSampleIds() {
        when(repository.findAll()).thenReturn(List.of(loan(1L, "Existing", 100L)));
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.resetToSample(List.of(new LoanDTO(77L, "Sample", 500L,
                List.of(new LoanRepaymentDTO(88L, LocalDate.of(2026, 11, 1), 100L, 10L)))));

        verify(repository, times(2)).deleteAll(org.mockito.ArgumentMatchers.any(Iterable.class));
        ArgumentCaptor<List<Loan>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals("Sample", captor.getValue().get(0).getName());
        assertEquals(100L, captor.getValue().get(0).getRepayments().get(0).getPrincipalAmount());
    }

    @Test
    void resetToDefaultSampleBuildsThreeMonthlyRepayments() {
        when(repository.findAll()).thenReturn(new ArrayList<>());
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.resetToDefaultSample();

        ArgumentCaptor<List<Loan>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        Loan saved = captor.getValue().get(0);
        assertEquals("Khoản vay mẫu", saved.getName());
        assertEquals(120_000_000L, saved.getOpeningPrincipal());
        assertEquals(3, saved.getRepayments().size());
        assertEquals(1_200_000L, saved.getRepayments().get(0).getInterestAmount());
        assertEquals(LocalDate.of(2027, 1, 31), saved.getRepayments().get(2).getDueDate());
    }

    private Loan loan(Long id, String name, Long principal) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setName(name);
        loan.setOpeningPrincipal(principal);
        return loan;
    }

    private LoanRepayment repayment(Long id, String dueDate, Long principal, Long interest) {
        LoanRepayment payment = new LoanRepayment();
        payment.setId(id);
        payment.setDueDate(LocalDate.parse(dueDate));
        payment.setPrincipalAmount(principal);
        payment.setInterestAmount(interest);
        return payment;
    }
}
