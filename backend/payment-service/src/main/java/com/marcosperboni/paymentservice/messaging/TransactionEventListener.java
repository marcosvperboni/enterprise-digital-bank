package com.marcosperboni.paymentservice.messaging;

import com.marcosperboni.paymentservice.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventListener {

	private final PaymentService paymentService;

	public TransactionEventListener(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@KafkaListener(topics = "transaction-events", groupId = "payment-service")
	public void onTransactionCreated(TransactionCreatedEvent event) {
		// Processed synchronously so the consumer offset only commits once the payment
		// has been persisted and the resulting PaymentProcessedEvent published.
		paymentService.processTransactionEvent(event).block();
	}
}
