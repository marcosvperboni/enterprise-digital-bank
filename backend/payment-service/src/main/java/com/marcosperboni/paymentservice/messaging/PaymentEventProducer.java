package com.marcosperboni.paymentservice.messaging;

import reactor.core.publisher.Mono;

public interface PaymentEventProducer {

	Mono<Void> publishPaymentProcessed(PaymentProcessedEvent event);
}
