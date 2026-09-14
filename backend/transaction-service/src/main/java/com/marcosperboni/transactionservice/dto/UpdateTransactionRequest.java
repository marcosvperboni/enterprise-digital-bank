package com.marcosperboni.transactionservice.dto;

import com.marcosperboni.transactionservice.domain.TransactionStatus;

public record UpdateTransactionRequest(String description, TransactionStatus status) {
}
