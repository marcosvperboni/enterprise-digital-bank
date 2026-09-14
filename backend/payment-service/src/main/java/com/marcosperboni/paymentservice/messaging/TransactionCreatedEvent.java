package com.marcosperboni.paymentservice.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreatedEvent(
		UUID transactionId,
		UUID accountId,
		TransactionType type,
		BigDecimal amount,
		UUID destinationAccountId) {
}
