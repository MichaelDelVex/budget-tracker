package com.budgettracker.importing;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface CsvTransactionParser {

    boolean supports(String originalFilename, List<String> header);

    ParsedTransactionFile parse(InputStream inputStream) throws IOException;
}
