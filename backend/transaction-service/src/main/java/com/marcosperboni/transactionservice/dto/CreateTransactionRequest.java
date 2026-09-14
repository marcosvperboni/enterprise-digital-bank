package com.marcosperboni.transactionservice.dto;

import com.marcosperboni.transactionservice.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(
		@NotNull UUID accountId,
		@NotNull TransactionType type,
		@NotNull @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than zero") BigDecimal amount,
		String currency,
		UUID destinationAccountId,
		String description) {
}
