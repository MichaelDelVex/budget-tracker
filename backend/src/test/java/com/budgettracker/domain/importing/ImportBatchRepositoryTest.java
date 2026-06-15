package com.budgettracker.domain.importing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportBatchRepositoryTest {

    @Autowired
    private ImportBatchRepository importBatchRepository;

    @Test
    void savesAndFindsImportBatchByStatus() {
        String filename = "statement-" + UUID.randomUUID() + ".csv";
        importBatchRepository.save(new ImportBatch(filename, ImportBatchStatus.PENDING, 12));

        assertThat(importBatchRepository.findByStatus(ImportBatchStatus.PENDING))
            .extracting(ImportBatch::getSourceFilename)
            .contains(filename);
    }
}
