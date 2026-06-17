package com.budgettracker.domain.rule;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorisationRuleRepository extends JpaRepository<CategorisationRule, Integer> {

    List<CategorisationRule> findAllByOrderByPriorityAscIdAsc();

    List<CategorisationRule> findByActiveTrueOrderByPriorityAscIdAsc();
}
