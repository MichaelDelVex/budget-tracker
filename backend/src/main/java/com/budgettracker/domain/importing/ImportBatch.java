package com.budgettracker.domain.importing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "import_batch")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Min(1)
    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Min(0)
    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Min(0)
    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Min(0)
    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount;

    @Min(0)
    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ImportBatch() {
    }

    public ImportBatch(
        Integer accountId,
        String originalFilename,
        int totalRows,
        int importedCount,
        int duplicateCount,
        int failedCount,
        Instant importedAt
    ) {
        this.accountId = accountId;
        this.originalFilename = originalFilename;
        this.totalRows = totalRows;
        this.importedCount = importedCount;
        this.duplicateCount = duplicateCount;
        this.failedCount = failedCount;
        this.importedAt = importedAt;
    }

    public Integer getId() {
        return id;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateCounts(int importedCount, int duplicateCount, int failedCount) {
        this.importedCount = importedCount;
        this.duplicateCount = duplicateCount;
        this.failedCount = failedCount;
    }
}
