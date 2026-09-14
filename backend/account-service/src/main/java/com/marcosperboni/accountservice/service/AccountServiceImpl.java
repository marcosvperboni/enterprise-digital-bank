package com.marcosperboni.accountservice.service;

import com.marcosperboni.accountservice.domain.Account;
import com.marcosperboni.accountservice.domain.AccountStatus;
import com.marcosperboni.accountservice.dto.AccountCreateRequest;
import com.marcosperboni.accountservice.dto.AccountResponse;
import com.marcosperboni.accountservice.dto.AccountUpdateRequest;
import com.marcosperboni.accountservice.dto.AdjustBalanceRequest;
import com.marcosperboni.accountservice.exception.ConflictException;
import com.marcosperboni.accountservice.exception.ResourceNotFoundException;
import com.marcosperboni.accountservice.exception.UnprocessableBalanceException;
import com.marcosperboni.accountservice.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountServiceImpl implements AccountService {

    private static final int ACCOUNT_NUMBER_MAX_ATTEMPTS = 5;

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public AccountResponse create(AccountCreateRequest request) {
        Account account = new Account();
        account.setCustomerId(request.customerId());
        account.setAccountType(request.accountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(BigDecimal.ZERO);
        account.setAccountNumber(generateUniqueAccountNumber());
        return toResponse(accountRepository.save(account));
    }

    @Override
    public Page<AccountResponse> findAll(Pageable pageable) {
        return accountRepository.findAll(pageable).map(AccountServiceImpl::toResponse);
    }

    @Override
    public AccountResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Override
    @Transactional
    public AccountResponse update(UUID id, AccountUpdateRequest request) {
        Account account = getOrThrow(id);
        account.setAccountType(request.accountType());
        account.setStatus(request.status());
        return toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Account account = getOrThrow(id);
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ConflictException("Cannot close account " + id + " with non-zero balance: " + account.getBalance());
        }
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public AccountResponse adjustBalance(UUID id, AdjustBalanceRequest request) {
        Account account = getOrThrow(id);
        BigDecimal newBalance = account.getBalance().add(request.amount());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new UnprocessableBalanceException("Adjustment would result in a negative balance for account " + id);
        }
        account.setBalance(newBalance);
        return toResponse(accountRepository.save(account));
    }

    private Account getOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    // ponytail: random 10-digit candidate + existence retry loop instead of a sequence table; fine at this volume,
    // swap for a DB sequence/dedicated generator if account creation throughput ever gets meaningful.
    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < ACCOUNT_NUMBER_MAX_ATTEMPTS; attempt++) {
            String candidate = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique account number after " + ACCOUNT_NUMBER_MAX_ATTEMPTS + " attempts");
    }

    private static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
