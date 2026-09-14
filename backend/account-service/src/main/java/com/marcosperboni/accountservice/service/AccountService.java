package com.marcosperboni.accountservice.service;

import com.marcosperboni.accountservice.dto.AccountCreateRequest;
import com.marcosperboni.accountservice.dto.AccountResponse;
import com.marcosperboni.accountservice.dto.AccountUpdateRequest;
import com.marcosperboni.accountservice.dto.AdjustBalanceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AccountService {

    AccountResponse create(AccountCreateRequest request);

    Page<AccountResponse> findAll(Pageable pageable);

    AccountResponse findById(UUID id);

    AccountResponse update(UUID id, AccountUpdateRequest request);

    void delete(UUID id);

    AccountResponse adjustBalance(UUID id, AdjustBalanceRequest request);
}
