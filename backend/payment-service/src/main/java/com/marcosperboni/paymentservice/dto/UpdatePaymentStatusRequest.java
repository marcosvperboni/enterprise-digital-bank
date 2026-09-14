package com.marcosperboni.paymentservice.dto;

import com.marcosperboni.paymentservice.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePaymentStatusRequest(@NotNull PaymentStatus status) {
}
