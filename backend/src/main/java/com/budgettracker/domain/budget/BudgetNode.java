package com.budgettracker.domain.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "budget_node")
public class BudgetNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Min(1)
    @Column(name = "budget_profile_id", nullable = false)
    private Integer budgetProfileId;

    @Min(1)
    @Column(name = "parent_node_id")
    private Integer parentNodeId;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false)
    private String name;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Column(nullable = false)
    private BigDecimal percentage;

    @Min(1)
    @Column(name = "category_id")
    private Integer categoryId;

    @Min(0)
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BudgetNode() {
    }

    public BudgetNode(
        Integer budgetProfileId,
        Integer parentNodeId,
        String name,
        BigDecimal percentage,
        Integer categoryId,
        int sortOrder
    ) {
        this.budgetProfileId = budgetProfileId;
        this.parentNodeId = parentNodeId;
        this.name = name;
        this.percentage = percentage;
        this.categoryId = categoryId;
        this.sortOrder = sortOrder;
    }

    public Integer getId() {
        return id;
    }

    public Integer getBudgetProfileId() {
        return budgetProfileId;
    }

    public Integer getParentNodeId() {
        return parentNodeId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
        Integer parentNodeId,
        String name,
        BigDecimal percentage,
        Integer categoryId,
        int sortOrder
    ) {
        this.parentNodeId = parentNodeId;
        this.name = name;
        this.percentage = percentage;
        this.categoryId = categoryId;
        this.sortOrder = sortOrder;
    }
}
