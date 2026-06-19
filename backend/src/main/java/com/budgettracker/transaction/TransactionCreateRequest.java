package com.budgettracker.transaction;

import com.budgettracker.domain.transaction.TransactionDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record TransactionCreateRequest(
    @NotNull @Min(1) Integer accountId,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate transactionDate,
    @NotBlank @Size(max = 255) String description,
    @NotBlank @Size(max = 255) String rawDescription,
    @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
    @NotNull TransactionDirection direction,
    @Min(1) Integer categoryId,
    @Min(1) Integer tagId
) {
}
