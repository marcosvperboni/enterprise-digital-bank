package com.marcosperboni.paymentservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KafkaPaymentEventProducer implements PaymentEventProducer {

	private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventProducer.class);
	private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

	private final KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;

	public KafkaPaymentEventProducer(KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public Mono<Void> publishPaymentProcessed(PaymentProcessedEvent event) {
		log.info("Publishing PaymentProcessedEvent for payment {} (transaction {}, status {})",
				event.paymentId(), event.transactionId(), event.status());
		return Mono.fromFuture(() -> kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event.paymentId().toString(), event).toCompletableFuture())
				.then();
	}
}
