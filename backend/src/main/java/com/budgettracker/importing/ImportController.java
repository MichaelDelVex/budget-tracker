package com.budgettracker.importing;

import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final TransactionImportService transactionImportService;

    public ImportController(TransactionImportService transactionImportService) {
        this.transactionImportService = transactionImportService;
    }

    @PostMapping(value = "/transactions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportSummaryResponse importTransactions(
        @RequestParam @Min(1) Integer accountId,
        @RequestPart MultipartFile file
    ) {
        return transactionImportService.importTransactions(accountId, file);
    }
}
