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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private TransactionEventProducer eventProducer;

	private TransactionService transactionService;

	private TransactionService service() {
		if (transactionService == null) {
			transactionService = new TransactionService(transactionRepository, eventProducer);
		}
		return transactionService;
	}

	@Test
	void create_persistsPendingTransactionAndPublishesEvent() {
		UUID accountId = UUID.randomUUID();
		CreateTransactionRequest request = new CreateTransactionRequest(
				accountId, TransactionType.DEPOSIT, new BigDecimal("100.00"), null, null, "salary");

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction transaction = invocation.getArgument(0);
			transaction.setId(UUID.randomUUID());
			return transaction;
		});

		TransactionDto result = service().create(request);

		assertThat(result.status()).isEqualTo(TransactionStatus.PENDING);
		assertThat(result.currency()).isEqualTo("BRL");
		assertThat(result.amount()).isEqualByComparingTo("100.00");

		ArgumentCaptor<TransactionCreatedEvent> captor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
		verify(eventProducer).publishTransactionCreated(captor.capture());
		assertThat(captor.getValue().accountId()).isEqualTo(accountId);
		assertThat(captor.getValue().type()).isEqualTo(TransactionType.DEPOSIT);
	}

	@Test
	void create_transferRequiresDestinationAccountId() {
		UUID accountId = UUID.randomUUID();
		CreateTransactionRequest request = new CreateTransactionRequest(
				accountId, TransactionType.TRANSFER, new BigDecimal("50.00"), "BRL", null, null);

		assertThatThrownBy(() -> service().create(request)).isInstanceOf(InvalidTransactionException.class);
		verify(transactionRepository, never()).save(any());
		verify(eventProducer, never()).publishTransactionCreated(any());
	}

	@Test
	void create_transferToSameAccountIsRejected() {
		UUID accountId = UUID.randomUUID();
		CreateTransactionRequest request = new CreateTransactionRequest(
				accountId, TransactionType.TRANSFER, new BigDecimal("50.00"), "BRL", accountId, null);

		assertThatThrownBy(() -> service().create(request)).isInstanceOf(InvalidTransactionException.class);
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void findById_returnsDto() {
		UUID id = UUID.randomUUID();
		Transaction transaction = new Transaction();
		transaction.setId(id);
		transaction.setAccountId(UUID.randomUUID());
		transaction.setType(TransactionType.DEPOSIT);
		transaction.setAmount(BigDecimal.TEN);
		when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));

		TransactionDto result = service().findById(id);

		assertThat(result.id()).isEqualTo(id);
	}

	@Test
	void findById_throwsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(transactionRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().findById(id)).isInstanceOf(TransactionNotFoundException.class);
	}

	@Test
	void findAll_filtersByAccountIdWhenProvided() {
		UUID accountId = UUID.randomUUID();
		Pageable pageable = PageRequest.of(0, 10);
		Page<Transaction> page = new PageImpl<>(List.of());
		when(transactionRepository.findByAccountId(accountId, pageable)).thenReturn(page);

		service().findAll(accountId, pageable);

		verify(transactionRepository).findByAccountId(accountId, pageable);
		verify(transactionRepository, never()).findAll(pageable);
	}

	@Test
	void update_changesDescriptionAndStatus() {
		UUID id = UUID.randomUUID();
		Transaction transaction = new Transaction();
		transaction.setId(id);
		transaction.setStatus(TransactionStatus.PENDING);
		when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TransactionDto result = service().update(id, new UpdateTransactionRequest("updated", TransactionStatus.COMPLETED));

		assertThat(result.description()).isEqualTo("updated");
		assertThat(result.status()).isEqualTo(TransactionStatus.COMPLETED);
	}

	@Test
	void update_throwsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(transactionRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().update(id, new UpdateTransactionRequest("x", null)))
				.isInstanceOf(TransactionNotFoundException.class);
	}

	@Test
	void delete_removesPendingTransaction() {
		UUID id = UUID.randomUUID();
		Transaction transaction = new Transaction();
		transaction.setId(id);
		transaction.setStatus(TransactionStatus.PENDING);
		when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));

		service().delete(id);

		verify(transactionRepository).delete(transaction);
	}

	@Test
	void delete_rejectsNonPendingTransaction() {
		UUID id = UUID.randomUUID();
		Transaction transaction = new Transaction();
		transaction.setId(id);
		transaction.setStatus(TransactionStatus.COMPLETED);
		when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));

		assertThatThrownBy(() -> service().delete(id)).isInstanceOf(TransactionNotDeletableException.class);
		verify(transactionRepository, never()).delete(any(Transaction.class));
	}

	@Test
	void delete_throwsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(transactionRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().delete(id)).isInstanceOf(TransactionNotFoundException.class);
	}
}
