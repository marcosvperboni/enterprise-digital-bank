package com.marcosperboni.accountservice.dto;

import com.marcosperboni.accountservice.domain.AccountType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccountCreateRequest(
        @NotNull UUID customerId,
        @NotNull AccountType accountType
) {
}
