package com.budgettracker.domain.budget;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetProfileRepository extends JpaRepository<BudgetProfile, Integer> {

    List<BudgetProfile> findAllByOrderByNameAsc();

    List<BudgetProfile> findAllByActiveTrue();
}
