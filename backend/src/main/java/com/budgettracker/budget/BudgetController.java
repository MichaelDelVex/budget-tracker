package com.budgettracker.budget;

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
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/profiles")
    public List<BudgetProfileResponse> listProfiles() {
        return budgetService.listProfiles();
    }

    @PostMapping("/profiles")
    public ResponseEntity<BudgetProfileResponse> createProfile(
        @Valid @RequestBody BudgetProfileRequest request
    ) {
        BudgetProfileResponse profile = budgetService.createProfile(request);
        return ResponseEntity
            .created(URI.create("/api/budgets/profiles/" + profile.id()))
            .body(profile);
    }

    @PutMapping("/profiles/{id}")
    public BudgetProfileResponse updateProfile(
        @PathVariable Integer id,
        @Valid @RequestBody BudgetProfileRequest request
    ) {
        return budgetService.updateProfile(id, request);
    }

    @DeleteMapping("/profiles/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Integer id) {
        budgetService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profiles/{profileId}/nodes")
    public List<BudgetNodeResponse> listNodes(@PathVariable Integer profileId) {
        return budgetService.listNodes(profileId);
    }

    @PostMapping("/profiles/{profileId}/nodes")
    public ResponseEntity<BudgetNodeResponse> createNode(
        @PathVariable Integer profileId,
        @Valid @RequestBody BudgetNodeRequest request
    ) {
        BudgetNodeResponse node = budgetService.createNode(profileId, request);
        return ResponseEntity
            .created(URI.create("/api/budgets/nodes/" + node.id()))
            .body(node);
    }

    @PutMapping("/nodes/{id}")
    public BudgetNodeResponse updateNode(
        @PathVariable Integer id,
        @Valid @RequestBody BudgetNodeRequest request
    ) {
        return budgetService.updateNode(id, request);
    }

    @DeleteMapping("/nodes/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable Integer id) {
        budgetService.deleteNode(id);
        return ResponseEntity.noContent().build();
    }
}
