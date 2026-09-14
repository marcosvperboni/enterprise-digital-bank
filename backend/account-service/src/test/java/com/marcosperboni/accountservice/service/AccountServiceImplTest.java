package com.marcosperboni.accountservice.service;

import com.marcosperboni.accountservice.domain.Account;
import com.marcosperboni.accountservice.domain.AccountStatus;
import com.marcosperboni.accountservice.domain.AccountType;
import com.marcosperboni.accountservice.dto.AccountCreateRequest;
import com.marcosperboni.accountservice.dto.AccountResponse;
import com.marcosperboni.accountservice.dto.AccountUpdateRequest;
import com.marcosperboni.accountservice.dto.AdjustBalanceRequest;
import com.marcosperboni.accountservice.exception.ConflictException;
import com.marcosperboni.accountservice.exception.ResourceNotFoundException;
import com.marcosperboni.accountservice.exception.UnprocessableBalanceException;
import com.marcosperboni.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountServiceImpl service() {
        return new AccountServiceImpl(accountRepository);
    }

    private static Account existingAccount(UUID id, BigDecimal balance, AccountStatus status) {
        Account account = new Account();
        account.setId(id);
        account.setCustomerId(UUID.randomUUID());
        account.setAccountNumber("1234567890");
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(balance);
        account.setStatus(status);
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        return account;
    }

    @Test
    void createGeneratesAccountNumberAndSavesWithZeroBalance() {
        UUID customerId = UUID.randomUUID();
        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setCreatedAt(Instant.now());
            a.setUpdatedAt(Instant.now());
            return a;
        });

        AccountResponse response = service().create(new AccountCreateRequest(customerId, AccountType.SAVINGS));

        assertThat(response.id()).isNotNull();
        assertThat(response.accountNumber()).isNotBlank();
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.customerId()).isEqualTo(customerId);
    }

    @Test
    void findByIdReturnsResponseWhenAccountExists() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(existingAccount(id, BigDecimal.ZERO, AccountStatus.ACTIVE)));

        AccountResponse response = service().findById(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateChangesTypeAndStatusButNeverBalance() {
        UUID id = UUID.randomUUID();
        Account existing = existingAccount(id, new BigDecimal("100.00"), AccountStatus.ACTIVE);
        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = service().update(id, new AccountUpdateRequest(AccountType.SAVINGS, AccountStatus.BLOCKED));

        assertThat(response.accountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(response.status()).isEqualTo(AccountStatus.BLOCKED);
        assertThat(response.balance()).isEqualByComparingTo("100.00");
    }

    @Test
    void updateThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(id, new AccountUpdateRequest(AccountType.SAVINGS, AccountStatus.ACTIVE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteSoftClosesWhenBalanceIsZero() {
        UUID id = UUID.randomUUID();
        Account existing = existingAccount(id, BigDecimal.ZERO, AccountStatus.ACTIVE);
        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));

        service().delete(id);

        assertThat(existing.getStatus()).isEqualTo(AccountStatus.CLOSED);
        verify(accountRepository).save(existing);
    }

    @Test
    void deleteThrowsConflictWhenBalanceIsNotZero() {
        UUID id = UUID.randomUUID();
        Account existing = existingAccount(id, new BigDecimal("50.00"), AccountStatus.ACTIVE);
        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service().delete(id))
                .isInstanceOf(ConflictException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void deleteThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adjustBalanceCreditsAndReturnsUpdatedBalance() {
        UUID id = UUID.randomUUID();
        Account existing = existingAccount(id, new BigDecimal("100.00"), AccountStatus.ACTIVE);
        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = service().adjustBalance(id, new AdjustBalanceRequest(new BigDecimal("25.00"), "deposit"));

        assertThat(response.balance()).isEqualByComparingTo("125.00");
    }

    @Test
    void adjustBalanceDebitsAndReturnsUpdatedBalance() {
        UUID id = UUID.randomUUID();
        Account existing = existingAccount(id, new BigDecimal("100.00"), AccountStatus.ACTIVE);
        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = service().adjustBalance(id, new AdjustBalanceRequest(new BigDecimal("-40.00"), "withdrawal"));

        assertThat(response.balance()).isEqualByComparingTo("60.00");
    }

    @Test
    void adjustBalanceThrowsUnprocessableWhenResultWouldBeNegative() {
        UUID id = UUID.randomUUID();
        Account existing = existingAccount(id, new BigDecimal("10.00"), AccountStatus.ACTIVE);
        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service().adjustBalance(id, new AdjustBalanceRequest(new BigDecimal("-50.00"), "withdrawal")))
                .isInstanceOf(UnprocessableBalanceException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void adjustBalanceThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().adjustBalance(id, new AdjustBalanceRequest(BigDecimal.TEN, "deposit")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
