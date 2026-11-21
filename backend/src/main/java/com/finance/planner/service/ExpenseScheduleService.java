package com.finance.planner.service;

import com.finance.planner.dto.ExpenseChangeDTO;
import com.finance.planner.dto.ExpenseDTO;
import com.finance.planner.entity.ExpenseAmountChange;
import com.finance.planner.entity.ExpenseCategory;
import com.finance.planner.repository.ExpenseCategoryRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseScheduleService {
    private final ExpenseCategoryRepository repository;

    public ExpenseScheduleService(ExpenseCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ExpenseDTO> getExpenses() {
        return repository.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
    }

    public List<ExpenseDTO> saveExpenses(List<ExpenseDTO> expenses) {
        repository.deleteAll(repository.findAll());
        repository.flush();
        LocalDateTime now = LocalDateTime.now();
        List<ExpenseCategory> entities = new ArrayList<>();
        for (ExpenseDTO dto : expenses) {
            ExpenseCategory category = new ExpenseCategory();
            category.setName(dto.getName().trim());
            category.setCreatedAt(now);
            category.setUpdatedAt(now);
            for (ExpenseChangeDTO changeDto : dto.getChanges()) {
                ExpenseAmountChange change = new ExpenseAmountChange();
                change.setEffectiveMonth(YearMonth.parse(changeDto.getEffectiveMonth()).atDay(1));
                change.setAmount(changeDto.getAmount());
                category.addChange(change);
            }
            entities.add(category);
        }
        return repository.saveAll(entities).stream().map(this::toDto).toList();
    }

    public List<ExpenseDTO> seedDefaults() {
        return saveExpenses(defaultExpenses());
    }

    public static List<ExpenseDTO> defaultExpenses() {
        return List.of(
                new ExpenseDTO("Sinh hoạt phí", List.of(new ExpenseChangeDTO("2026-11", 4_250_000L))),
                new ExpenseDTO("Quỹ nuôi con", List.of(
                        new ExpenseChangeDTO("2026-11", 2_500_000L),
                        new ExpenseChangeDTO("2027-01", 2_750_000L),
                        new ExpenseChangeDTO("2027-04", 3_250_000L))),
                new ExpenseDTO("Tiền nhà", List.of(
                        new ExpenseChangeDTO("2026-11", 3_750_000L),
                        new ExpenseChangeDTO("2027-02", 5_750_000L))));
    }

    private ExpenseDTO toDto(ExpenseCategory entity) {
        return new ExpenseDTO(entity.getName(), entity.getChanges().stream()
                .map(change -> new ExpenseChangeDTO(YearMonth.from(change.getEffectiveMonth()).toString(), change.getAmount()))
                .toList());
    }
}
