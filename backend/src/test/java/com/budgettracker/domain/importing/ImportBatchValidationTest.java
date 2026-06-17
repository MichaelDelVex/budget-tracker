package com.budgettracker.domain.importing;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ImportBatchValidationTest extends ValidationTestSupport {

    @Test
    void requiresAccountFilenameAndNonNegativeCounts() {
        ImportBatch importBatch = new ImportBatch(0, "", -1, -1, -1, -1, Instant.now());

        assertThat(invalidProperties(importBatch))
            .contains("accountId", "originalFilename", "totalRows", "importedCount", "duplicateCount", "failedCount");
    }
}
