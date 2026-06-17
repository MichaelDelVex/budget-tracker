package com.budgettracker.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionUpdateRequestValidationTest extends ValidationTestSupport {

    @Test
    void requiresCoreFieldsAndPositiveAmount() {
        TransactionUpdateRequest request = new TransactionUpdateRequest(
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

        assertThat(invalidProperties(request))
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
