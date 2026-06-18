package com.budgettracker.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.budgettracker.category.CategoryNotFoundException;
import com.budgettracker.domain.budget.BudgetNode;
import com.budgettracker.domain.budget.BudgetNodeRepository;
import com.budgettracker.domain.budget.BudgetProfile;
import com.budgettracker.domain.budget.BudgetProfileRepository;
import com.budgettracker.domain.category.CategoryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetProfileRepository profileRepository;

    @Mock
    private BudgetNodeRepository nodeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void listsProfiles() {
        when(profileRepository.findAllByOrderByNameAsc()).thenReturn(List.of(profile(1, "Default", false)));

        assertThat(budgetService.listProfiles())
            .extracting(BudgetProfileResponse::name)
            .containsExactly("Default");
    }

    @Test
    void createsDraftProfile() {
        when(profileRepository.save(any(BudgetProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        budgetService.createProfile(new BudgetProfileRequest("Default", "Main plan", false));

        ArgumentCaptor<BudgetProfile> captor = ArgumentCaptor.forClass(BudgetProfile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Default");
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void activatingProfileRequiresCompleteTopLevelSiblingsAndDeactivatesOthers() {
        BudgetProfile profile = profile(1, "Default", false);
        BudgetProfile other = profile(2, "Old", true);
        when(profileRepository.findById(1)).thenReturn(Optional.of(profile));
        when(profileRepository.findAllByActiveTrue()).thenReturn(List.of(other));
        when(nodeRepository.findAllByBudgetProfileId(1)).thenReturn(List.of(
            node(10, 1, null, "Savings", "40.00", null),
            node(11, 1, null, "Spending", "60.00", null)
        ));

        BudgetProfileResponse response = budgetService.updateProfile(
            1,
            new BudgetProfileRequest("Default", "Main plan", true)
        );

        assertThat(response.active()).isTrue();
        assertThat(other.isActive()).isFalse();
    }

    @Test
    void rejectsActivatingIncompleteProfile() {
        BudgetProfile profile = profile(1, "Default", false);
        when(profileRepository.findById(1)).thenReturn(Optional.of(profile));
        when(nodeRepository.findAllByBudgetProfileId(1)).thenReturn(List.of(
            node(10, 1, null, "Savings", "40.00", null)
        ));

        assertThatThrownBy(() -> budgetService.updateProfile(
            1,
            new BudgetProfileRequest("Default", "Main plan", true)
        )).isInstanceOf(BudgetValidationException.class)
            .hasMessageContaining("total 100%");
    }

    @Test
    void createsNode() {
        BudgetProfile profile = profile(1, "Default", false);
        when(profileRepository.findById(1)).thenReturn(Optional.of(profile));
        when(categoryRepository.existsById(2)).thenReturn(true);
        when(nodeRepository.findAllByBudgetProfileId(1)).thenReturn(List.of());
        when(nodeRepository.save(any(BudgetNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetNodeResponse response = budgetService.createNode(
            1,
            new BudgetNodeRequest(null, "Savings", new BigDecimal("40.00"), 2, 10)
        );

        assertThat(response.name()).isEqualTo("Savings");
        assertThat(response.categoryId()).isEqualTo(2);
    }

    @Test
    void rejectsSiblingTotalsOverOneHundred() {
        BudgetProfile profile = profile(1, "Default", false);
        when(profileRepository.findById(1)).thenReturn(Optional.of(profile));
        when(nodeRepository.findAllByBudgetProfileId(1)).thenReturn(List.of(
            node(10, 1, null, "Savings", "60.00", null)
        ));

        assertThatThrownBy(() -> budgetService.createNode(
            1,
            new BudgetNodeRequest(null, "Spending", new BigDecimal("50.00"), null, 20)
        )).isInstanceOf(BudgetValidationException.class)
            .hasMessageContaining("must not exceed 100%");
    }

    @Test
    void allowsChildNodeWhenParentBelongsToSameProfile() {
        BudgetProfile profile = profile(1, "Default", false);
        BudgetNode parent = node(10, 1, null, "Spending", "60.00", null);
        when(profileRepository.findById(1)).thenReturn(Optional.of(profile));
        when(nodeRepository.findById(10)).thenReturn(Optional.of(parent));
        when(nodeRepository.findAllByBudgetProfileId(1)).thenReturn(List.of(parent));
        when(nodeRepository.save(any(BudgetNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetNodeResponse response = budgetService.createNode(
            1,
            new BudgetNodeRequest(10, "Groceries", new BigDecimal("50.00"), null, 10)
        );

        assertThat(response.parentNodeId()).isEqualTo(10);
    }

    @Test
    void rejectsParentFromDifferentProfile() {
        BudgetProfile profile = profile(1, "Default", false);
        BudgetNode parent = node(10, 2, null, "Other", "100.00", null);
        when(profileRepository.findById(1)).thenReturn(Optional.of(profile));
        when(nodeRepository.findById(10)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> budgetService.createNode(
            1,
            new BudgetNodeRequest(10, "Groceries", new BigDecimal("50.00"), null, 10)
        )).isInstanceOf(BudgetValidationException.class)
            .hasMessageContaining("same budget profile");
    }

    @Test
    void rejectsMissingCategory() {
        BudgetProfile profile = profile(1, "Default", false);
        when(profileRepository.findById(1)).thenReturn(Optional.of(profile));
        when(categoryRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> budgetService.createNode(
            1,
            new BudgetNodeRequest(null, "Groceries", new BigDecimal("50.00"), 99, 10)
        )).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void updatesAndDeletesNode() {
        BudgetNode node = node(10, 1, null, "Old", "40.00", null);
        BudgetProfile profile = profile(1, "Default", false);
        when(nodeRepository.findById(10)).thenReturn(Optional.of(node));
        when(nodeRepository.findAllByBudgetProfileId(1)).thenReturn(List.of(node));
        when(profileRepository.findById(1)).thenReturn(Optional.of(profile));

        BudgetNodeResponse response = budgetService.updateNode(
            10,
            new BudgetNodeRequest(null, "New", new BigDecimal("45.00"), null, 5)
        );
        budgetService.deleteNode(10);

        assertThat(response.name()).isEqualTo("New");
        verify(nodeRepository).deleteById(10);
    }

    private BudgetProfile profile(Integer id, String name, boolean active) {
        BudgetProfile profile = new BudgetProfile(name, null, active);
        ReflectionTestUtils.setField(profile, "id", id);
        return profile;
    }

    private BudgetNode node(
        Integer id,
        Integer profileId,
        Integer parentNodeId,
        String name,
        String percentage,
        Integer categoryId
    ) {
        BudgetNode node = new BudgetNode(
            profileId,
            parentNodeId,
            name,
            new BigDecimal(percentage),
            categoryId,
            10
        );
        ReflectionTestUtils.setField(node, "id", id);
        return node;
    }
}
