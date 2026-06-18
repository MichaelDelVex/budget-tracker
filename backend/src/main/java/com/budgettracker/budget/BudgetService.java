package com.budgettracker.budget;

import com.budgettracker.category.CategoryNotFoundException;
import com.budgettracker.domain.budget.BudgetNode;
import com.budgettracker.domain.budget.BudgetNodeRepository;
import com.budgettracker.domain.budget.BudgetProfile;
import com.budgettracker.domain.budget.BudgetProfileRepository;
import com.budgettracker.domain.category.CategoryRepository;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private final BudgetProfileRepository profileRepository;
    private final BudgetNodeRepository nodeRepository;
    private final CategoryRepository categoryRepository;

    public BudgetService(
        BudgetProfileRepository profileRepository,
        BudgetNodeRepository nodeRepository,
        CategoryRepository categoryRepository
    ) {
        this.profileRepository = profileRepository;
        this.nodeRepository = nodeRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetProfileResponse> listProfiles() {
        return profileRepository.findAllByOrderByNameAsc().stream()
            .map(BudgetProfileResponse::from)
            .toList();
    }

    @Transactional
    public BudgetProfileResponse createProfile(BudgetProfileRequest request) {
        BudgetProfile profile = profileRepository.save(new BudgetProfile(
            request.name(),
            request.description(),
            false
        ));

        if (request.active()) {
            activateProfile(profile);
        }

        return BudgetProfileResponse.from(profile);
    }

    @Transactional
    public BudgetProfileResponse updateProfile(Integer id, BudgetProfileRequest request) {
        BudgetProfile profile = findProfile(id);
        profile.update(request.name(), request.description(), request.active());

        if (request.active()) {
            activateProfile(profile);
        }

        return BudgetProfileResponse.from(profile);
    }

    @Transactional
    public void deleteProfile(Integer id) {
        if (!profileRepository.existsById(id)) {
            throw new BudgetProfileNotFoundException(id);
        }

        profileRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<BudgetNodeResponse> listNodes(Integer profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new BudgetProfileNotFoundException(profileId);
        }

        return nodeRepository.findAllByBudgetProfileIdOrderByParentNodeIdAscSortOrderAscNameAsc(profileId).stream()
            .map(BudgetNodeResponse::from)
            .toList();
    }

    @Transactional
    public BudgetNodeResponse createNode(Integer profileId, BudgetNodeRequest request) {
        BudgetProfile profile = findProfile(profileId);
        validateCategory(request.categoryId());
        validateParent(profileId, request.parentNodeId());
        validateSiblingTotal(profileId, null, request.parentNodeId(), request.percentage());

        BudgetNode node = nodeRepository.save(new BudgetNode(
            profileId,
            request.parentNodeId(),
            request.name(),
            request.percentage(),
            request.categoryId(),
            request.sortOrder()
        ));
        validateActiveProfile(profile);
        return BudgetNodeResponse.from(node);
    }

    @Transactional
    public BudgetNodeResponse updateNode(Integer id, BudgetNodeRequest request) {
        BudgetNode node = nodeRepository.findById(id)
            .orElseThrow(() -> new BudgetNodeNotFoundException(id));
        validateCategory(request.categoryId());
        validateParent(node.getBudgetProfileId(), request.parentNodeId());
        validateNoCycle(node, request.parentNodeId());
        validateSiblingTotal(node.getBudgetProfileId(), node.getId(), request.parentNodeId(), request.percentage());

        node.update(
            request.parentNodeId(),
            request.name(),
            request.percentage(),
            request.categoryId(),
            request.sortOrder()
        );
        validateActiveProfile(findProfile(node.getBudgetProfileId()));
        return BudgetNodeResponse.from(node);
    }

    @Transactional
    public void deleteNode(Integer id) {
        BudgetNode node = nodeRepository.findById(id)
            .orElseThrow(() -> new BudgetNodeNotFoundException(id));
        BudgetProfile profile = findProfile(node.getBudgetProfileId());
        nodeRepository.deleteById(id);
        validateActiveProfile(profile);
    }

    private void activateProfile(BudgetProfile profile) {
        validateCompleteProfile(profile.getId());
        profileRepository.findAllByActiveTrue().stream()
            .filter(activeProfile -> !Objects.equals(activeProfile.getId(), profile.getId()))
            .forEach(BudgetProfile::deactivate);
        profile.update(profile.getName(), profile.getDescription(), true);
    }

    private void validateActiveProfile(BudgetProfile profile) {
        if (profile.isActive()) {
            validateCompleteProfile(profile.getId());
        }
    }

    private void validateCompleteProfile(Integer profileId) {
        List<BudgetNode> nodes = nodeRepository.findAllByBudgetProfileId(profileId);
        if (nodes.isEmpty()) {
            throw new BudgetValidationException("An active budget profile must have at least one node.");
        }

        validateSiblingGroups(nodes, true);
    }

    private void validateSiblingTotal(
        Integer profileId,
        Integer currentNodeId,
        Integer parentNodeId,
        BigDecimal requestedPercentage
    ) {
        List<BudgetNode> siblings = nodeRepository.findAllByBudgetProfileId(profileId).stream()
            .filter(node -> Objects.equals(node.getParentNodeId(), parentNodeId))
            .filter(node -> !Objects.equals(node.getId(), currentNodeId))
            .toList();
        BigDecimal total = siblings.stream()
            .map(BudgetNode::getPercentage)
            .reduce(requestedPercentage, BigDecimal::add);

        if (total.compareTo(ONE_HUNDRED) > 0) {
            throw new BudgetValidationException("Sibling budget percentages must not exceed 100%.");
        }
    }

    private void validateSiblingGroups(List<BudgetNode> nodes, boolean requireExactTotal) {
        Map<Integer, List<BudgetNode>> byParent = new HashMap<>();
        List<BudgetNode> rootNodes = new ArrayList<>();
        for (BudgetNode node : nodes) {
            if (node.getParentNodeId() == null) {
                rootNodes.add(node);
            } else {
                byParent.computeIfAbsent(node.getParentNodeId(), ignored -> new ArrayList<>()).add(node);
            }
        }
        validateSiblingGroup(rootNodes, requireExactTotal);
        byParent.entrySet().stream()
            .filter(entry -> entry.getKey() != null)
            .forEach(entry -> validateSiblingGroup(entry.getValue(), requireExactTotal));
    }

    private void validateSiblingGroup(List<BudgetNode> siblings, boolean requireExactTotal) {
        if (siblings == null || siblings.isEmpty()) {
            return;
        }

        BigDecimal total = siblings.stream()
            .map(BudgetNode::getPercentage)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (requireExactTotal && total.compareTo(ONE_HUNDRED) != 0) {
            throw new BudgetValidationException("Active budget sibling percentages must total 100%.");
        }
        if (total.compareTo(ONE_HUNDRED) > 0) {
            throw new BudgetValidationException("Sibling budget percentages must not exceed 100%.");
        }
    }

    private BudgetProfile findProfile(Integer id) {
        return profileRepository.findById(id)
            .orElseThrow(() -> new BudgetProfileNotFoundException(id));
    }

    private void validateCategory(Integer categoryId) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }
    }

    private void validateParent(Integer profileId, Integer parentNodeId) {
        if (parentNodeId == null) {
            return;
        }

        BudgetNode parent = nodeRepository.findById(parentNodeId)
            .orElseThrow(() -> new BudgetNodeNotFoundException(parentNodeId));
        if (!Objects.equals(parent.getBudgetProfileId(), profileId)) {
            throw new BudgetValidationException("Parent node must belong to the same budget profile.");
        }
    }

    private void validateNoCycle(BudgetNode node, Integer parentNodeId) {
        if (parentNodeId == null) {
            return;
        }
        if (Objects.equals(node.getId(), parentNodeId)) {
            throw new BudgetValidationException("A budget node cannot be its own parent.");
        }

        Map<Integer, BudgetNode> nodesById = nodeRepository.findAllByBudgetProfileId(node.getBudgetProfileId()).stream()
            .collect(Collectors.toMap(BudgetNode::getId, nodeInProfile -> nodeInProfile));
        ArrayDeque<Integer> parents = new ArrayDeque<>();
        parents.add(parentNodeId);
        while (!parents.isEmpty()) {
            BudgetNode parent = nodesById.get(parents.removeFirst());
            if (parent == null || parent.getParentNodeId() == null) {
                continue;
            }
            if (Objects.equals(parent.getParentNodeId(), node.getId())) {
                throw new BudgetValidationException("Budget node parent would create a cycle.");
            }
            parents.add(parent.getParentNodeId());
        }
    }
}
