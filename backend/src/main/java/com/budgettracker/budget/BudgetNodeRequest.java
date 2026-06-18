package com.budgettracker.budget;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BudgetNodeRequest(
    @Min(1)
    Integer parentNodeId,

    @NotBlank
    @Size(max = 120)
    String name,

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    BigDecimal percentage,

    @Min(1)
    Integer categoryId,

    @Min(0)
    int sortOrder
) {
}
