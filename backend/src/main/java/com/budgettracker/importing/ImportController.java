package com.budgettracker.importing;

import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final TransactionImportService transactionImportService;
    private final CsvCategoryMappingService csvCategoryMappingService;

    public ImportController(
        TransactionImportService transactionImportService,
        CsvCategoryMappingService csvCategoryMappingService
    ) {
        this.transactionImportService = transactionImportService;
        this.csvCategoryMappingService = csvCategoryMappingService;
    }

    @PostMapping(value = "/transactions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportSummaryResponse importTransactions(
        @RequestParam @Min(1) Integer accountId,
        @RequestPart MultipartFile file
    ) {
        return transactionImportService.importTransactions(accountId, file);
    }

    @PostMapping("/csv-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CsvCategoryMappingResponse createCsvCategoryMapping(@Valid @RequestBody CsvCategoryMappingRequest request) {
        return csvCategoryMappingService.createMapping(request);
    }
}
