package com.marcosperboni.paymentservice.messaging;

import com.marcosperboni.paymentservice.domain.PaymentStatus;

import java.util.UUID;

public record PaymentProcessedEvent(UUID paymentId, UUID transactionId, PaymentStatus status) {
}
