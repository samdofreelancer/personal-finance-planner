package com.finance.planner.service;

import com.finance.planner.dto.LoanDTO;
import com.finance.planner.dto.LoanRepaymentDTO;
import com.finance.planner.entity.Loan;
import com.finance.planner.entity.LoanRepayment;
import com.finance.planner.repository.LoanRepository;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoanScheduleService {
    private final LoanRepository repository;

    public LoanScheduleService(LoanRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<LoanDTO> getLoans() {
        return repository.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
    }

    public List<LoanDTO> replaceLoans(List<LoanDTO> requestedLoans) {
        if (requestedLoans == null) throw new IllegalArgumentException("Danh sách khoản vay không được để trống.");

        Map<Long, Loan> existingLoans = new HashMap<>();
        repository.findAllByOrderByIdAsc().forEach(loan -> existingLoans.put(loan.getId(), loan));
        Set<Long> seenLoanIds = new HashSet<>();
        List<Loan> updatedLoans = new ArrayList<>();

        for (LoanDTO requested : requestedLoans) {
            validateLoan(requested);
            Loan loan;
            if (requested.id() == null) {
                loan = new Loan();
                loan.setCreatedAt(LocalDateTime.now());
            } else {
                if (!seenLoanIds.add(requested.id())) throw new IllegalArgumentException("Khoản vay bị lặp ID.");
                loan = existingLoans.remove(requested.id());
                if (loan == null) throw new IllegalArgumentException("Khoản vay không tồn tại hoặc không thuộc dữ liệu hiện tại.");
            }

            loan.setName(requested.name().trim());
            loan.setOpeningPrincipal(requested.openingPrincipal());
            loan.setUpdatedAt(LocalDateTime.now());
            replaceRepayments(loan, requested.repayments());
            updatedLoans.add(loan);
        }

        repository.deleteAll(existingLoans.values());
        return repository.saveAll(updatedLoans).stream().map(this::toDto).toList();
    }

    public void resetToSample(List<LoanDTO> sample) {
        repository.deleteAll(repository.findAll());
        replaceLoans(sample.stream().map(loan -> new LoanDTO(null, loan.name(), loan.openingPrincipal(),
                loan.repayments().stream().map(payment -> new LoanRepaymentDTO(null, payment.dueDate(),
                        payment.principalAmount(), payment.interestAmount())).toList())).toList());
    }

    public void resetToDefaultSample() {
        long opening = 120_000_000L;
        long fixedPayment = 10_000_000L;
        long novemberInterest = 1_200_000L;
        long decemberInterest = 1_100_000L;
        long novemberPrincipal = Math.min(opening, Math.max(0, fixedPayment - novemberInterest));
        long afterNovember = Math.max(0, opening - novemberPrincipal);
        long decemberPrincipal = Math.min(afterNovember, Math.max(0, fixedPayment - decemberInterest));
        long januaryPrincipal = Math.max(0, afterNovember - decemberPrincipal);
        List<LoanRepaymentDTO> repayments = List.of(
                new LoanRepaymentDTO(null, LocalDate.of(2026, 11, 30), novemberPrincipal, novemberInterest),
                new LoanRepaymentDTO(null, LocalDate.of(2026, 12, 31), decemberPrincipal, decemberInterest),
                new LoanRepaymentDTO(null, LocalDate.of(2027, 1, 31), januaryPrincipal, 1_000_000L));
        resetToSample(List.of(new LoanDTO(null, "Khoản vay mẫu", opening, repayments)));
    }

    private void replaceRepayments(Loan loan, List<LoanRepaymentDTO> requested) {
        if (requested == null) throw new IllegalArgumentException("Danh sách kỳ trả không được để trống.");
        Map<Long, LoanRepayment> existing = new HashMap<>();
        loan.getRepayments().forEach(payment -> existing.put(payment.getId(), payment));
        Set<Long> seen = new HashSet<>();
        List<LoanRepayment> updated = new ArrayList<>();
        for (LoanRepaymentDTO dto : requested) {
            validateRepayment(dto);
            LoanRepayment payment;
            if (dto.id() == null) {
                payment = new LoanRepayment();
            } else {
                if (!seen.add(dto.id())) throw new IllegalArgumentException("Kỳ trả bị lặp ID.");
                payment = existing.remove(dto.id());
                if (payment == null) throw new IllegalArgumentException("Kỳ trả không tồn tại hoặc không thuộc khoản vay.");
            }
            payment.setDueDate(dto.dueDate());
            payment.setPrincipalAmount(dto.principalAmount());
            payment.setInterestAmount(dto.interestAmount());
            updated.add(payment);
        }
        existing.values().forEach(loan.getRepayments()::remove);
        updated.stream().filter(payment -> !loan.getRepayments().contains(payment)).forEach(loan::addRepayment);
    }

    private void validateLoan(LoanDTO loan) {
        if (loan == null || loan.name() == null || loan.name().isBlank() || loan.name().trim().length() > 120)
            throw new IllegalArgumentException("Tên khoản vay phải có từ 1 đến 120 ký tự.");
        if (loan.openingPrincipal() == null || loan.openingPrincipal() < 0)
            throw new IllegalArgumentException("Dư nợ gốc phải là số VND nguyên không âm.");
    }

    private void validateRepayment(LoanRepaymentDTO payment) {
        if (payment == null || payment.dueDate() == null)
            throw new IllegalArgumentException("Ngày trả nợ không hợp lệ.");
        if (payment.principalAmount() == null || payment.principalAmount() < 0)
            throw new IllegalArgumentException("Tiền gốc phải là số VND nguyên không âm.");
        if (payment.interestAmount() == null || payment.interestAmount() < 0)
            throw new IllegalArgumentException("Tiền lãi phải là số VND nguyên không âm.");
    }

    private LoanDTO toDto(Loan loan) {
        return new LoanDTO(loan.getId(), loan.getName(), loan.getOpeningPrincipal(),
                loan.getRepayments().stream()
                        .sorted(java.util.Comparator.comparing(LoanRepayment::getDueDate).thenComparing(LoanRepayment::getId))
                        .map(payment -> new LoanRepaymentDTO(payment.getId(), payment.getDueDate(),
                                payment.getPrincipalAmount(), payment.getInterestAmount()))
                        .toList());
    }
}
