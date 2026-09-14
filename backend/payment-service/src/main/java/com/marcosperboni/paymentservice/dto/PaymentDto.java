package com.marcosperboni.paymentservice.dto;

import com.marcosperboni.paymentservice.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentDto(
		UUID id,
		UUID transactionId,
		BigDecimal amount,
		PaymentStatus status,
		Instant processedAt,
		Instant createdAt) {
}
