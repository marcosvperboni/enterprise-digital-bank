package com.marcosperboni.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
		@NotNull UUID transactionId,
		@NotNull @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than zero") BigDecimal amount) {
}
