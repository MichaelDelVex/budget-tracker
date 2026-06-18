package com.budgettracker.account;

import com.budgettracker.domain.account.Account;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.importing.ImportBatchRepository;
import com.budgettracker.domain.transaction.TransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ImportBatchRepository importBatchRepository;

    public AccountService(
        AccountRepository accountRepository,
        TransactionRepository transactionRepository,
        ImportBatchRepository importBatchRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.importBatchRepository = importBatchRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts() {
        return accountRepository.findAll().stream()
            .map(AccountResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Integer id) {
        return accountRepository.findById(id)
            .map(AccountResponse::from)
            .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        Account account = new Account(request.name(), request.bank(), request.accountType());
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse updateAccount(Integer id, AccountRequest request) {
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));

        account.update(request.name(), request.bank(), request.accountType());
        return AccountResponse.from(account);
    }

    @Transactional
    public void deleteAccount(Integer id) {
        if (!accountRepository.existsById(id)) {
            throw new AccountNotFoundException(id);
        }

        accountRepository.deleteById(id);
    }

    @Transactional
    public void deleteAccountWithTransactions(Integer id) {
        if (!accountRepository.existsById(id)) {
            throw new AccountNotFoundException(id);
        }

        transactionRepository.deleteByAccountId(id);
        importBatchRepository.deleteByAccountId(id);
        accountRepository.deleteById(id);
    }

}
