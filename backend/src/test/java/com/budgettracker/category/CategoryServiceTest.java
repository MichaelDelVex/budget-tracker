package com.budgettracker.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.category.CategoryType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void listsCategories() {
        when(categoryRepository.findAllByOrderBySortOrderAscNameAsc())
            .thenReturn(List.of(new Category("Groceries", CategoryType.EXPENSE, true, 10)));

        assertThat(categoryService.listCategories())
            .extracting(CategoryResponse::name)
            .containsExactly("Groceries");
    }

    @Test
    void createsCategory() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.createCategory(new CategoryRequest("Fuel", CategoryType.EXPENSE, false, true, 20));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Fuel");
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void updatesCategory() {
        Category category = new Category("Old", CategoryType.EXPENSE, false, 10);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.updateCategory(
            1,
            new CategoryRequest("New", CategoryType.INCOME, true, false, 5)
        );

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.type()).isEqualTo(CategoryType.INCOME);
        assertThat(response.active()).isFalse();
    }

    @Test
    void throwsWhenUpdatingMissingCategory() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(
            99,
            new CategoryRequest("New", CategoryType.EXPENSE, false, true, 1)
        )).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void deletesCategory() {
        when(categoryRepository.existsById(1)).thenReturn(true);

        categoryService.deleteCategory(1);

        verify(categoryRepository).deleteById(1);
    }
}
