package com.budgettracker.domain.importing;

import com.budgettracker.domain.category.CategoryType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsvCategoryMappingRepository extends JpaRepository<CsvCategoryMapping, Integer> {

    Optional<CsvCategoryMapping> findByNormalizedSourceNameAndType(String normalizedSourceName, CategoryType type);
}
