package com.budgettracker.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.category.CategoryType;
import com.budgettracker.domain.importing.CsvCategoryMapping;
import com.budgettracker.domain.importing.CsvCategoryMappingRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CsvCategoryMappingServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CsvCategoryMappingRepository csvCategoryMappingRepository;

    @Test
    void createsCategoryAndMappingForEditedCsvCategoryName() {
        CsvCategoryMappingService service = new CsvCategoryMappingService(categoryRepository, csvCategoryMappingRepository);
        Category savedCategory = category(12, "Parking", CategoryType.EXPENSE);
        when(categoryRepository.findByNameIgnoreCaseAndActiveTrue("Parking")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(csvCategoryMappingRepository.findByNormalizedSourceNameAndType("parking & tolls", CategoryType.EXPENSE))
            .thenReturn(Optional.empty());

        CsvCategoryMappingResponse response = service.createMapping(new CsvCategoryMappingRequest(
            "Parking & tolls",
            "Parking",
            CategoryType.EXPENSE
        ));

        ArgumentCaptor<CsvCategoryMapping> mapping = ArgumentCaptor.forClass(CsvCategoryMapping.class);
        verify(csvCategoryMappingRepository).save(mapping.capture());
        assertThat(response.category().name()).isEqualTo("Parking");
        assertThat(mapping.getValue().getSourceName()).isEqualTo("Parking & tolls");
        assertThat(mapping.getValue().getNormalizedSourceName()).isEqualTo("parking & tolls");
        assertThat(mapping.getValue().getCategoryId()).isEqualTo(12);
    }

    private Category category(Integer id, String name, CategoryType type) {
        Category category = new Category(name, type, false, 500);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
