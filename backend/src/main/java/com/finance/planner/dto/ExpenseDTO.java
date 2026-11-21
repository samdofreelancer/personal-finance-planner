package com.finance.planner.dto;

import java.util.ArrayList;
import java.util.List;

public class ExpenseDTO {
    private String name;
    private List<ExpenseChangeDTO> changes = new ArrayList<>();

    public ExpenseDTO() {}

    public ExpenseDTO(String name, List<ExpenseChangeDTO> changes) {
        this.name = name;
        this.changes = changes;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ExpenseChangeDTO> getChanges() { return changes; }
    public void setChanges(List<ExpenseChangeDTO> changes) { this.changes = changes; }
}
