package com.budgettracker.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.budgettracker.domain.account.Account;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.account.AccountType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void listsAccounts() {
        when(accountRepository.findAll())
            .thenReturn(List.of(new Account("Everyday", "Example Bank", AccountType.CHECKING)));

        assertThat(accountService.listAccounts())
            .extracting(AccountResponse::name)
            .containsExactly("Everyday");
    }

    @Test
    void getsAccountById() {
        when(accountRepository.findById(1))
            .thenReturn(Optional.of(new Account("Savings", "Example Bank", AccountType.SAVINGS)));

        assertThat(accountService.getAccount(1).accountType()).isEqualTo(AccountType.SAVINGS);
    }

    @Test
    void throwsWhenAccountIsMissing() {
        when(accountRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(99))
            .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void createsAccount() {
        AccountRequest request = new AccountRequest("Credit", "Example Bank", AccountType.CREDIT_CARD);
        when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        accountService.createAccount(request);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Credit");
        assertThat(captor.getValue().getBank()).isEqualTo("Example Bank");
    }

    @Test
    void updatesAccount() {
        Account account = new Account("Old", "Old Bank", AccountType.CASH);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.updateAccount(
            1,
            new AccountRequest("New", "New Bank", AccountType.OTHER)
        );

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.bank()).isEqualTo("New Bank");
        assertThat(response.accountType()).isEqualTo(AccountType.OTHER);
    }

    @Test
    void deletesAccount() {
        when(accountRepository.existsById(1)).thenReturn(true);

        accountService.deleteAccount(1);

        verify(accountRepository).deleteById(1);
    }

    @Test
    void throwsWhenDeletingMissingAccount() {
        when(accountRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> accountService.deleteAccount(99))
            .isInstanceOf(AccountNotFoundException.class);
    }
}
