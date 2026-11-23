package com.finance.planner.household;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bootstrap_lock")
@Getter
@NoArgsConstructor
public class BootstrapLock {

    @Id
    private Long id;

    public BootstrapLock(Long id) {
        this.id = id;
    }
}
