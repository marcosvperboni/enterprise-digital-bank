package com.marcosperboni.transactionservice.dto;

import com.marcosperboni.transactionservice.domain.TransactionStatus;
import com.marcosperboni.transactionservice.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDto(
		UUID id,
		UUID accountId,
		TransactionType type,
		BigDecimal amount,
		String currency,
		UUID destinationAccountId,
		TransactionStatus status,
		String description,
		Instant createdAt) {
}
