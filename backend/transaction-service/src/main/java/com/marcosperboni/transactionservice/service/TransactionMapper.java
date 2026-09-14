package com.marcosperboni.transactionservice.service;

import com.marcosperboni.transactionservice.domain.Transaction;
import com.marcosperboni.transactionservice.dto.TransactionDto;

final class TransactionMapper {

	private TransactionMapper() {
	}

	static TransactionDto toDto(Transaction transaction) {
		return new TransactionDto(
				transaction.getId(),
				transaction.getAccountId(),
				transaction.getType(),
				transaction.getAmount(),
				transaction.getCurrency(),
				transaction.getDestinationAccountId(),
				transaction.getStatus(),
				transaction.getDescription(),
				transaction.getCreatedAt());
	}
}
