package com.budgettracker.domain.importing;

import com.budgettracker.domain.category.CategoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "csv_category_mapping",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_csv_category_mapping_source_type",
        columnNames = {"normalized_source_name", "type"}
    )
)
public class CsvCategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 120)
    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @NotBlank
    @Size(max = 120)
    @Column(name = "normalized_source_name", nullable = false)
    private String normalizedSourceName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @Min(1)
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CsvCategoryMapping() {
    }

    public CsvCategoryMapping(String sourceName, CategoryType type, Integer categoryId) {
        this.sourceName = sourceName.trim();
        this.normalizedSourceName = normalise(sourceName);
        this.type = type;
        this.categoryId = categoryId;
    }

    public Integer getId() {
        return id;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getNormalizedSourceName() {
        return normalizedSourceName;
    }

    public CategoryType getType() {
        return type;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void updateCategory(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public static String normalise(String sourceName) {
        return sourceName.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
