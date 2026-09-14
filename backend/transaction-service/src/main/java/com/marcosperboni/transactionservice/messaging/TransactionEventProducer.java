package com.marcosperboni.transactionservice.messaging;

public interface TransactionEventProducer {

	void publishTransactionCreated(TransactionCreatedEvent event);
}
