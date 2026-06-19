package com.budgettracker.importing;

import com.budgettracker.category.CategoryResponse;
import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.importing.CsvCategoryMapping;
import com.budgettracker.domain.importing.CsvCategoryMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CsvCategoryMappingService {

    private final CategoryRepository categoryRepository;
    private final CsvCategoryMappingRepository csvCategoryMappingRepository;

    public CsvCategoryMappingService(
        CategoryRepository categoryRepository,
        CsvCategoryMappingRepository csvCategoryMappingRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.csvCategoryMappingRepository = csvCategoryMappingRepository;
    }

    @Transactional
    public CsvCategoryMappingResponse createMapping(CsvCategoryMappingRequest request) {
        Category category = categoryRepository.findByNameIgnoreCaseAndActiveTrue(request.categoryName())
            .map(existing -> {
                if (existing.getType() != request.type()) {
                    throw new CsvImportException("Existing category type does not match the CSV category type.");
                }
                return existing;
            })
            .orElseGet(() -> categoryRepository.save(new Category(
                request.categoryName().trim(),
                request.type(),
                false,
                500
            )));

        csvCategoryMappingRepository.findByNormalizedSourceNameAndType(
            CsvCategoryMapping.normalise(request.sourceName()),
            request.type()
        ).ifPresentOrElse(
            mapping -> mapping.updateCategory(category.getId()),
            () -> csvCategoryMappingRepository.save(new CsvCategoryMapping(
                request.sourceName(),
                request.type(),
                category.getId()
            ))
        );

        return new CsvCategoryMappingResponse(request.sourceName().trim(), CategoryResponse.from(category));
    }
}
