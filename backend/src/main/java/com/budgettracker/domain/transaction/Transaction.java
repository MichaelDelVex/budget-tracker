package com.budgettracker.domain.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "transaction_record")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Min(1)
    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String description;

    @NotBlank
    @Size(max = 255)
    @Column(name = "raw_description", nullable = false)
    private String rawDescription;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(nullable = false)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionDirection direction;

    @Min(1)
    @Column(name = "category_id")
    private Integer categoryId;

    @Min(1)
    @Column(name = "tag_id")
    private Integer tagId;

    @Min(1)
    @Column(name = "import_batch_id")
    private Integer importBatchId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transaction() {
    }

    public Transaction(
        Integer accountId,
        LocalDate transactionDate,
        String description,
        String rawDescription,
        BigDecimal amount,
        TransactionDirection direction,
        Integer categoryId,
        Integer tagId,
        Integer importBatchId
    ) {
        this.accountId = accountId;
        this.transactionDate = transactionDate;
        this.description = description;
        this.rawDescription = rawDescription;
        this.amount = amount;
        this.direction = direction;
        this.categoryId = categoryId;
        this.tagId = tagId;
        this.importBatchId = importBatchId;
    }

    public Integer getId() {
        return id;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public String getRawDescription() {
        return rawDescription;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionDirection getDirection() {
        return direction;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public Integer getTagId() {
        return tagId;
    }

    public Integer getImportBatchId() {
        return importBatchId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
        Integer accountId,
        LocalDate transactionDate,
        String description,
        String rawDescription,
        BigDecimal amount,
        TransactionDirection direction,
        Integer categoryId,
        Integer tagId,
        Integer importBatchId
    ) {
        this.accountId = accountId;
        this.transactionDate = transactionDate;
        this.description = description;
        this.rawDescription = rawDescription;
        this.amount = amount;
        this.direction = direction;
        this.categoryId = categoryId;
        this.tagId = tagId;
        this.importBatchId = importBatchId;
    }
}
