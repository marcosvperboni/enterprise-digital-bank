package com.marcosperboni.paymentservice.service;

import com.marcosperboni.paymentservice.domain.Payment;
import com.marcosperboni.paymentservice.dto.PaymentDto;

final class PaymentMapper {

	private PaymentMapper() {
	}

	static PaymentDto toDto(Payment payment) {
		return new PaymentDto(
				payment.getId(),
				payment.getTransactionId(),
				payment.getAmount(),
				payment.getStatus(),
				payment.getProcessedAt(),
				payment.getCreatedAt());
	}
}
