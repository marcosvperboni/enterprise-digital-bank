package com.marcosperboni.accountservice.dto;

import com.marcosperboni.accountservice.domain.AccountStatus;
import com.marcosperboni.accountservice.domain.AccountType;
import jakarta.validation.constraints.NotNull;

public record AccountUpdateRequest(
        @NotNull AccountType accountType,
        @NotNull AccountStatus status
) {
}
