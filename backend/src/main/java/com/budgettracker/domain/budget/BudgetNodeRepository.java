package com.budgettracker.domain.budget;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetNodeRepository extends JpaRepository<BudgetNode, Integer> {

    List<BudgetNode> findAllByBudgetProfileIdOrderByParentNodeIdAscSortOrderAscNameAsc(Integer budgetProfileId);

    List<BudgetNode> findAllByBudgetProfileId(Integer budgetProfileId);
}
