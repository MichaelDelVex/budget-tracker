package com.budgettracker.web;

import com.budgettracker.account.AccountNotFoundException;
import com.budgettracker.budget.BudgetNodeNotFoundException;
import com.budgettracker.budget.BudgetProfileNotFoundException;
import com.budgettracker.budget.BudgetValidationException;
import com.budgettracker.category.CategoryNotFoundException;
import com.budgettracker.importing.CsvImportException;
import com.budgettracker.importing.ImportBatchNotFoundException;
import com.budgettracker.rule.CategorisationRuleNotFoundException;
import com.budgettracker.tag.TagNotFoundException;
import com.budgettracker.importing.UnsupportedCsvFormatException;
import com.budgettracker.transaction.TransactionDuplicateException;
import com.budgettracker.transaction.TransactionNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AccountNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TransactionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(CategoryNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TagNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(CategorisationRuleNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(CategorisationRuleNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(ImportBatchNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ImportBatchNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(BudgetProfileNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(BudgetProfileNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(BudgetNodeNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(BudgetNodeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            fields.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(new ApiError("Validation failed", fields));
    }

    @ExceptionHandler({
        UnsupportedCsvFormatException.class,
        CsvImportException.class,
        BudgetValidationException.class,
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class,
        ConstraintViolationException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(ApiError.of("Request body is invalid or malformed."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ApiError.of("CSV file is too large. Use a file up to 5 MB."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.of(conflictMessage(exception)));
    }

    @ExceptionHandler(TransactionDuplicateException.class)
    public ResponseEntity<ApiError> handleConflict(TransactionDuplicateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(exception.getMessage()));
    }

    private String conflictMessage(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message == null) {
            message = exception.getMessage();
        }
        String lowerMessage = message == null ? "" : message.toLowerCase();

        if (lowerMessage.contains("transaction_record.account_id")
            && lowerMessage.contains("transaction_record.transaction_date")
            && lowerMessage.contains("transaction_record.description")
            && lowerMessage.contains("transaction_record.amount")) {
            return "This transaction already exists.";
        }

        if (lowerMessage.contains("account.name")) {
            return "An account with this name already exists.";
        }

        if (lowerMessage.contains("category.name")) {
            return "A category with this name already exists.";
        }

        if (lowerMessage.contains("tag.name")) {
            return "A tag with this name already exists.";
        }

        if (lowerMessage.contains("foreign key constraint failed")) {
            return "Cannot delete or update this resource because it is still referenced.";
        }

        return "Cannot save this change because it conflicts with existing data.";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        LOGGER.error("Unhandled API exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.of("Something went wrong. Check the server logs for details."));
    }
}
