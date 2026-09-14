package com.marcosperboni.transactionservice.exception;

import com.marcosperboni.transactionservice.domain.TransactionStatus;

import java.util.UUID;

public class TransactionNotDeletableException extends RuntimeException {

	public TransactionNotDeletableException(UUID id, TransactionStatus status) {
		super("Transaction " + id + " cannot be deleted, status=" + status);
	}
}
