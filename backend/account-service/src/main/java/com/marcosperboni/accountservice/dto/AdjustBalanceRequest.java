package com.marcosperboni.accountservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustBalanceRequest(
        @NotNull BigDecimal amount,
        @NotBlank String reason
) {
}
