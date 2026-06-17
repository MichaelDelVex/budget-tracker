package com.budgettracker.rule;

import com.budgettracker.category.CategoryNotFoundException;
import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.rule.CategorisationRule;
import com.budgettracker.domain.rule.CategorisationRuleRepository;
import com.budgettracker.domain.tag.Tag;
import com.budgettracker.domain.tag.TagRepository;
import com.budgettracker.tag.TagNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategorisationRuleService {

    private final CategorisationRuleRepository ruleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public CategorisationRuleService(
        CategorisationRuleRepository ruleRepository,
        CategoryRepository categoryRepository,
        TagRepository tagRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<CategorisationRuleResponse> listRules() {
        return ruleRepository.findAllByOrderByPriorityAscIdAsc().stream()
            .map(CategorisationRuleResponse::from)
            .toList();
    }

    @Transactional
    public CategorisationRuleResponse createRule(CategorisationRuleRequest request) {
        Category category = findCategory(request.categoryId());
        Tag tag = findTag(request.tagId());
        CategorisationRule rule = new CategorisationRule(
            request.matchText(),
            category,
            tag,
            request.active(),
            request.priority()
        );

        return CategorisationRuleResponse.from(ruleRepository.save(rule));
    }

    @Transactional
    public CategorisationRuleResponse updateRule(Integer id, CategorisationRuleRequest request) {
        CategorisationRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new CategorisationRuleNotFoundException(id));

        rule.update(
            request.matchText(),
            findCategory(request.categoryId()),
            findTag(request.tagId()),
            request.active(),
            request.priority()
        );
        return CategorisationRuleResponse.from(rule);
    }

    @Transactional
    public void deleteRule(Integer id) {
        if (!ruleRepository.existsById(id)) {
            throw new CategorisationRuleNotFoundException(id);
        }

        ruleRepository.deleteById(id);
    }

    private Category findCategory(Integer id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private Tag findTag(Integer id) {
        if (id == null) {
            return null;
        }

        return tagRepository.findById(id)
            .orElseThrow(() -> new TagNotFoundException(id));
    }
}
