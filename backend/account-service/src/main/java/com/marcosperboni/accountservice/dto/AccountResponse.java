package com.marcosperboni.accountservice.dto;

import com.marcosperboni.accountservice.domain.AccountStatus;
import com.marcosperboni.accountservice.domain.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String accountNumber,
        AccountType accountType,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
