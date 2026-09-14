package com.marcosperboni.transactionservice.messaging;

import com.marcosperboni.transactionservice.domain.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreatedEvent(
		UUID transactionId,
		UUID accountId,
		TransactionType type,
		BigDecimal amount,
		UUID destinationAccountId) {
}
