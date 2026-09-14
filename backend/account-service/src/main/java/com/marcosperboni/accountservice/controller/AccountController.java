package com.marcosperboni.accountservice.controller;

import com.marcosperboni.accountservice.dto.AccountCreateRequest;
import com.marcosperboni.accountservice.dto.AccountResponse;
import com.marcosperboni.accountservice.dto.AccountUpdateRequest;
import com.marcosperboni.accountservice.dto.AdjustBalanceRequest;
import com.marcosperboni.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountCreateRequest request) {
        return accountService.create(request);
    }

    @GetMapping
    public Page<AccountResponse> findAll(Pageable pageable) {
        return accountService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public AccountResponse findById(@PathVariable UUID id) {
        return accountService.findById(id);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable UUID id, @Valid @RequestBody AccountUpdateRequest request) {
        return accountService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        accountService.delete(id);
    }

    @PostMapping("/{id}/adjust-balance")
    public AccountResponse adjustBalance(@PathVariable UUID id, @Valid @RequestBody AdjustBalanceRequest request) {
        return accountService.adjustBalance(id, request);
    }
}
