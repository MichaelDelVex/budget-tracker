package com.budgettracker.domain.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionValidationTest extends ValidationTestSupport {

    @Test
    void requiresCoreFieldsAndPositiveAmount() {
        Transaction transaction = new Transaction(
            0,
            null,
            "",
            "",
            BigDecimal.ZERO,
            null,
            0,
            0,
            0
        );

        assertThat(invalidProperties(transaction))
            .contains(
                "accountId",
                "transactionDate",
                "description",
                "rawDescription",
                "amount",
                "direction",
                "categoryId",
                "tagId",
                "importBatchId"
            );
    }
}
