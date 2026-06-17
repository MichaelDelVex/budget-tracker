package com.budgettracker.domain.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    List<Category> findAllByOrderBySortOrderAscNameAsc();

    List<Category> findByDefaultCategoryTrueOrderBySortOrderAsc();

    Optional<Category> findByName(String name);

    Optional<Category> findByNameIgnoreCaseAndActiveTrue(String name);
}
