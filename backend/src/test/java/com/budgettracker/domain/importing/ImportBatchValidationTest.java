package com.budgettracker.domain.importing;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import org.junit.jupiter.api.Test;

class ImportBatchValidationTest extends ValidationTestSupport {

    @Test
    void requiresFilenameStatusAndNonNegativeRowCount() {
        ImportBatch importBatch = new ImportBatch("", null, -1);

        assertThat(invalidProperties(importBatch)).contains("sourceFilename", "status", "rowCount");
    }
}
