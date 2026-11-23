package com.finance.planner.service;

import com.finance.planner.dto.IncomeChangeDTO;
import com.finance.planner.entity.Assumption;
import com.finance.planner.entity.IncomeAmountChange;
import com.finance.planner.repository.IncomeAmountChangeRepository;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IncomeSchedulePersistenceService {
    private final IncomeAmountChangeRepository repository;

    public IncomeSchedulePersistenceService(IncomeAmountChangeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<IncomeChangeDTO> getIncomeChanges(Assumption assumption) {
        if (assumption.getId() == null) return List.of();
        return repository.findAllByAssumptionIdOrderByEffectiveMonthAsc(assumption.getId()).stream()
                .map(change -> new IncomeChangeDTO(YearMonth.from(change.getEffectiveMonth()).toString(), change.getAmount()))
                .toList();
    }

    public List<IncomeChangeDTO> saveIncomeChanges(Assumption assumption, List<IncomeChangeDTO> changes) {
        repository.deleteAllByAssumptionId(assumption.getId());
        repository.flush();
        List<IncomeAmountChange> entities = changes.stream().map(dto -> {
            IncomeAmountChange entity = new IncomeAmountChange();
            entity.setAssumption(assumption);
            entity.setEffectiveMonth(YearMonth.parse(dto.effectiveMonth()).atDay(1));
            entity.setAmount(dto.amount());
            return entity;
        }).toList();
        return repository.saveAll(entities).stream()
                .map(entity -> new IncomeChangeDTO(YearMonth.from(entity.getEffectiveMonth()).toString(), entity.getAmount()))
                .toList();
    }
}
