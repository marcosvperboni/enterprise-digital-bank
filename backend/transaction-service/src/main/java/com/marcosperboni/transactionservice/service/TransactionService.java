package com.marcosperboni.transactionservice.service;

import com.marcosperboni.transactionservice.domain.Transaction;
import com.marcosperboni.transactionservice.domain.TransactionStatus;
import com.marcosperboni.transactionservice.domain.TransactionType;
import com.marcosperboni.transactionservice.dto.CreateTransactionRequest;
import com.marcosperboni.transactionservice.dto.TransactionDto;
import com.marcosperboni.transactionservice.dto.UpdateTransactionRequest;
import com.marcosperboni.transactionservice.exception.InvalidTransactionException;
import com.marcosperboni.transactionservice.exception.TransactionNotDeletableException;
import com.marcosperboni.transactionservice.exception.TransactionNotFoundException;
import com.marcosperboni.transactionservice.messaging.TransactionCreatedEvent;
import com.marcosperboni.transactionservice.messaging.TransactionEventProducer;
import com.marcosperboni.transactionservice.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class TransactionService {

	private final TransactionRepository transactionRepository;
	private final TransactionEventProducer eventProducer;

	public TransactionService(TransactionRepository transactionRepository, TransactionEventProducer eventProducer) {
		this.transactionRepository = transactionRepository;
		this.eventProducer = eventProducer;
	}

	@Transactional
	public TransactionDto create(CreateTransactionRequest request) {
		validateTransfer(request.type(), request.accountId(), request.destinationAccountId());

		Transaction transaction = new Transaction();
		transaction.setAccountId(request.accountId());
		transaction.setType(request.type());
		transaction.setAmount(request.amount());
		transaction.setCurrency(StringUtils.hasText(request.currency()) ? request.currency() : "BRL");
		transaction.setDestinationAccountId(request.destinationAccountId());
		transaction.setDescription(request.description());
		transaction.setStatus(TransactionStatus.PENDING);

		Transaction saved = transactionRepository.save(transaction);
		eventProducer.publishTransactionCreated(new TransactionCreatedEvent(
				saved.getId(), saved.getAccountId(), saved.getType(), saved.getAmount(), saved.getDestinationAccountId()));
		return TransactionMapper.toDto(saved);
	}

	@Transactional(readOnly = true)
	public Page<TransactionDto> findAll(UUID accountId, Pageable pageable) {
		Page<Transaction> page = accountId != null
				? transactionRepository.findByAccountId(accountId, pageable)
				: transactionRepository.findAll(pageable);
		return page.map(TransactionMapper::toDto);
	}

	@Transactional(readOnly = true)
	public TransactionDto findById(UUID id) {
		return TransactionMapper.toDto(getOrThrow(id));
	}

	@Transactional
	public TransactionDto update(UUID id, UpdateTransactionRequest request) {
		Transaction transaction = getOrThrow(id);
		if (request.description() != null) {
			transaction.setDescription(request.description());
		}
		if (request.status() != null) {
			transaction.setStatus(request.status());
		}
		return TransactionMapper.toDto(transactionRepository.save(transaction));
	}

	@Transactional
	public void delete(UUID id) {
		Transaction transaction = getOrThrow(id);
		if (transaction.getStatus() != TransactionStatus.PENDING) {
			throw new TransactionNotDeletableException(id, transaction.getStatus());
		}
		transactionRepository.delete(transaction);
	}

	private Transaction getOrThrow(UUID id) {
		return transactionRepository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
	}

	private void validateTransfer(TransactionType type, UUID accountId, UUID destinationAccountId) {
		if (type != TransactionType.TRANSFER) {
			return;
		}
		if (destinationAccountId == null) {
			throw new InvalidTransactionException("destinationAccountId is required for TRANSFER transactions");
		}
		if (destinationAccountId.equals(accountId)) {
			throw new InvalidTransactionException("destinationAccountId must be different from accountId");
		}
	}
}
