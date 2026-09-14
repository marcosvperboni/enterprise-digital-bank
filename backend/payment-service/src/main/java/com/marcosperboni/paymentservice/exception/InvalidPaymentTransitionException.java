package com.marcosperboni.paymentservice.exception;

import com.marcosperboni.paymentservice.domain.PaymentStatus;

public class InvalidPaymentTransitionException extends RuntimeException {

	public InvalidPaymentTransitionException(PaymentStatus current, PaymentStatus target) {
		super("Cannot transition payment from " + current + " to " + target);
	}
}
