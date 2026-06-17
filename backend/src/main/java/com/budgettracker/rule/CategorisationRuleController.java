package com.budgettracker.rule;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorisation-rules")
public class CategorisationRuleController {

    private final CategorisationRuleService ruleService;

    public CategorisationRuleController(CategorisationRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public List<CategorisationRuleResponse> listRules() {
        return ruleService.listRules();
    }

    @PostMapping
    public ResponseEntity<CategorisationRuleResponse> createRule(
        @Valid @RequestBody CategorisationRuleRequest request
    ) {
        CategorisationRuleResponse rule = ruleService.createRule(request);
        return ResponseEntity
            .created(URI.create("/api/categorisation-rules/" + rule.id()))
            .body(rule);
    }

    @PutMapping("/{id}")
    public CategorisationRuleResponse updateRule(
        @PathVariable Integer id,
        @Valid @RequestBody CategorisationRuleRequest request
    ) {
        return ruleService.updateRule(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Integer id) {
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
