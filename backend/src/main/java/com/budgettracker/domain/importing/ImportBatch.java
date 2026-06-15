package com.budgettracker.domain.importing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "import_batch")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "source_filename", nullable = false)
    private String sourceFilename;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportBatchStatus status = ImportBatchStatus.PENDING;

    @Min(0)
    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "imported_at")
    private Instant importedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ImportBatch() {
    }

    public ImportBatch(String sourceFilename, ImportBatchStatus status, int rowCount) {
        this.sourceFilename = sourceFilename;
        this.status = status;
        this.rowCount = rowCount;
    }

    public Integer getId() {
        return id;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public ImportBatchStatus getStatus() {
        return status;
    }

    public int getRowCount() {
        return rowCount;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
