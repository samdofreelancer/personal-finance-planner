package com.finance.planner.service;

import com.finance.planner.dto.AssumptionDTO;
import com.finance.planner.entity.Assumption;
import com.finance.planner.repository.AssumptionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssumptionService {

    private final AssumptionRepository repository;

    public AssumptionService(AssumptionRepository repository) {
        this.repository = repository;
    }

    public AssumptionDTO getAssumptions() {
        Assumption entity = repository.findTopByOrderByIdAsc();
        if (entity == null) entity = seedDefaultsEntity();
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
        return toDTO(saved);
    }

    public AssumptionDTO seedDefaults() {
        Assumption saved = repository.save(seedDefaultsEntity());
        return toDTO(saved);
    }

    private Assumption seedDefaultsEntity() {
        Assumption entity = new Assumption();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private AssumptionDTO toDTO(Assumption e) {
        AssumptionDTO dto = new AssumptionDTO();
        dto.setBaseSalary(e.getBaseSalary());
        dto.setJanuaryMultiplier(e.getJanuaryMultiplier());
        dto.setLiving(e.getLiving());
        dto.setChildThrough2026(e.getChildThrough2026());
        dto.setChildQ12027(e.getChildQ12027());
        dto.setChildFromApr2027(e.getChildFromApr2027());
        dto.setRentThroughJan2027(e.getRentThroughJan2027());
        dto.setRentFromFeb2027(e.getRentFromFeb2027());
        dto.setLoanPrincipal(e.getLoanPrincipal());
        dto.setFixedPayment(e.getFixedPayment());
        dto.setNovInterest(e.getNovInterest());
        dto.setDecInterest(e.getDecInterest());
        dto.setPayoffInterest(e.getPayoffInterest());
        dto.setPayoffInterestConfirmed(e.getPayoffInterestConfirmed());
        dto.setStartingSavings(e.getStartingSavings());
        Map<Integer, BigDecimal> raises = new HashMap<>();
        raises.put(2027, e.getRaise2027());
        raises.put(2028, e.getRaise2028());
        raises.put(2029, e.getRaise2029());
        raises.put(2030, e.getRaise2030());
        dto.setRaises(raises);
        return dto;
    }

    private void updateEntityFromDTO(Assumption e, AssumptionDTO dto) {
        e.setBaseSalary(dto.getBaseSalary());
        e.setJanuaryMultiplier(dto.getJanuaryMultiplier());
        e.setLiving(dto.getLiving());
        e.setChildThrough2026(dto.getChildThrough2026());
        e.setChildQ12027(dto.getChildQ12027());
        e.setChildFromApr2027(dto.getChildFromApr2027());
        e.setRentThroughJan2027(dto.getRentThroughJan2027());
        e.setRentFromFeb2027(dto.getRentFromFeb2027());
        e.setLoanPrincipal(dto.getLoanPrincipal());
        e.setFixedPayment(dto.getFixedPayment());
        e.setNovInterest(dto.getNovInterest());
        e.setDecInterest(dto.getDecInterest());
        e.setPayoffInterest(dto.getPayoffInterest());
        e.setPayoffInterestConfirmed(dto.getPayoffInterestConfirmed());
        e.setStartingSavings(dto.getStartingSavings());
        if (dto.getRaises() != null) {
            e.setRaise2027(dto.getRaises().get(2027));
            e.setRaise2028(dto.getRaises().get(2028));
            e.setRaise2029(dto.getRaises().get(2029));
            e.setRaise2030(dto.getRaises().get(2030));
        }
    }
}