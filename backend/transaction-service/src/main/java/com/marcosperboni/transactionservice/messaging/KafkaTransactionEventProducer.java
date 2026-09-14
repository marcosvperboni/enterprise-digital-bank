package com.marcosperboni.transactionservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaTransactionEventProducer implements TransactionEventProducer {

	private static final Logger log = LoggerFactory.getLogger(KafkaTransactionEventProducer.class);
	private static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";

	private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

	public KafkaTransactionEventProducer(KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public void publishTransactionCreated(TransactionCreatedEvent event) {
		log.info("Publishing TransactionCreatedEvent for transaction {}", event.transactionId());
		kafkaTemplate.send(TRANSACTION_EVENTS_TOPIC, event.transactionId().toString(), event);
	}
}
