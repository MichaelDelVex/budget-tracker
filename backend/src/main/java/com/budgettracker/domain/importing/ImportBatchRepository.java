package com.budgettracker.domain.importing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Integer> {

    List<ImportBatch> findByAccountId(Integer accountId);
}
