package com.marcosperboni.paymentservice.service;

import com.marcosperboni.paymentservice.domain.Payment;
import com.marcosperboni.paymentservice.domain.PaymentStatus;
import com.marcosperboni.paymentservice.dto.CreatePaymentRequest;
import com.marcosperboni.paymentservice.dto.PaymentDto;
import com.marcosperboni.paymentservice.dto.UpdatePaymentStatusRequest;
import com.marcosperboni.paymentservice.exception.InvalidPaymentTransitionException;
import com.marcosperboni.paymentservice.exception.PaymentNotFoundException;
import com.marcosperboni.paymentservice.messaging.PaymentEventProducer;
import com.marcosperboni.paymentservice.messaging.PaymentProcessedEvent;
import com.marcosperboni.paymentservice.messaging.TransactionCreatedEvent;
import com.marcosperboni.paymentservice.messaging.TransactionType;
import com.marcosperboni.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentEventProducer eventProducer;

	private PaymentService service;

	private PaymentService service() {
		if (service == null) {
			service = new PaymentService(paymentRepository, eventProducer);
		}
		return service;
	}

	@Test
	void create_persistsPendingPayment() {
		UUID transactionId = UUID.randomUUID();
		CreatePaymentRequest request = new CreatePaymentRequest(transactionId, new BigDecimal("100.00"));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

		StepVerifier.create(service().create(request))
				.assertNext(dto -> {
					assertThat(dto.transactionId()).isEqualTo(transactionId);
					assertThat(dto.status()).isEqualTo(PaymentStatus.PENDING);
					assertThat(dto.amount()).isEqualByComparingTo("100.00");
				})
				.verifyComplete();
	}

	@Test
	void findAll_returnsPageOfPayments() {
		Pageable pageable = PageRequest.of(0, 10);
		Payment payment = paymentWith(PaymentStatus.PENDING);
		when(paymentRepository.findAllBy(pageable)).thenReturn(Flux.just(payment));
		when(paymentRepository.count()).thenReturn(Mono.just(1L));

		StepVerifier.create(service().findAll(pageable))
				.assertNext(page -> {
					assertThat(page.getTotalElements()).isEqualTo(1);
					assertThat(page.getContent()).hasSize(1);
				})
				.verifyComplete();
	}

	@Test
	void findById_returnsDtoWhenPresent() {
		Payment payment = paymentWith(PaymentStatus.PENDING);
		when(paymentRepository.findById(payment.getId())).thenReturn(Mono.just(payment));

		StepVerifier.create(service().findById(payment.getId()))
				.assertNext(dto -> assertThat(dto.id()).isEqualTo(payment.getId()))
				.verifyComplete();
	}

	@Test
	void findById_errorsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(paymentRepository.findById(id)).thenReturn(Mono.empty());

		StepVerifier.create(service().findById(id))
				.expectError(PaymentNotFoundException.class)
				.verify();
	}

	@Test
	void updateStatus_appliesValidTransition() {
		Payment payment = paymentWith(PaymentStatus.PENDING);
		when(paymentRepository.findById(payment.getId())).thenReturn(Mono.just(payment));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

		StepVerifier.create(service().updateStatus(payment.getId(), new UpdatePaymentStatusRequest(PaymentStatus.APPROVED)))
				.assertNext(dto -> {
					assertThat(dto.status()).isEqualTo(PaymentStatus.APPROVED);
					assertThat(dto.processedAt()).isNotNull();
				})
				.verifyComplete();
	}

	@Test
	void updateStatus_rejectsInvalidTransition() {
		Payment payment = paymentWith(PaymentStatus.APPROVED);
		when(paymentRepository.findById(payment.getId())).thenReturn(Mono.just(payment));

		StepVerifier.create(service().updateStatus(payment.getId(), new UpdatePaymentStatusRequest(PaymentStatus.REJECTED)))
				.expectError(InvalidPaymentTransitionException.class)
				.verify();
		verify(paymentRepository, never()).save(any());
	}

	@Test
	void updateStatus_errorsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(paymentRepository.findById(id)).thenReturn(Mono.empty());

		StepVerifier.create(service().updateStatus(id, new UpdatePaymentStatusRequest(PaymentStatus.APPROVED)))
				.expectError(PaymentNotFoundException.class)
				.verify();
	}

	@Test
	void delete_removesExistingPayment() {
		Payment payment = paymentWith(PaymentStatus.PENDING);
		when(paymentRepository.findById(payment.getId())).thenReturn(Mono.just(payment));
		when(paymentRepository.delete(payment)).thenReturn(Mono.empty());

		StepVerifier.create(service().delete(payment.getId())).verifyComplete();
		verify(paymentRepository).delete(payment);
	}

	@Test
	void delete_errorsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(paymentRepository.findById(id)).thenReturn(Mono.empty());

		StepVerifier.create(service().delete(id))
				.expectError(PaymentNotFoundException.class)
				.verify();
	}

	@Test
	void processTransactionEvent_approvesDepositAndPublishesEvent() {
		TransactionCreatedEvent event = new TransactionCreatedEvent(
				UUID.randomUUID(), UUID.randomUUID(), TransactionType.DEPOSIT, new BigDecimal("50.00"), null);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
		when(eventProducer.publishPaymentProcessed(any(PaymentProcessedEvent.class))).thenReturn(Mono.empty());

		StepVerifier.create(service().processTransactionEvent(event)).verifyComplete();

		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository, times(2)).save(paymentCaptor.capture());
		assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.APPROVED);
		assertThat(paymentCaptor.getValue().getProcessedAt()).isNotNull();

		ArgumentCaptor<PaymentProcessedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);
		verify(eventProducer).publishPaymentProcessed(eventCaptor.capture());
		assertThat(eventCaptor.getValue().transactionId()).isEqualTo(event.transactionId());
		assertThat(eventCaptor.getValue().status()).isEqualTo(PaymentStatus.APPROVED);
	}

	@Test
	void processTransactionEvent_approvesWithdrawal() {
		TransactionCreatedEvent event = new TransactionCreatedEvent(
				UUID.randomUUID(), UUID.randomUUID(), TransactionType.WITHDRAWAL, new BigDecimal("20.00"), null);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
		when(eventProducer.publishPaymentProcessed(any(PaymentProcessedEvent.class))).thenReturn(Mono.empty());

		StepVerifier.create(service().processTransactionEvent(event)).verifyComplete();
		verify(eventProducer).publishPaymentProcessed(any(PaymentProcessedEvent.class));
	}

	@Test
	void processTransactionEvent_ignoresTransfer() {
		TransactionCreatedEvent event = new TransactionCreatedEvent(
				UUID.randomUUID(), UUID.randomUUID(), TransactionType.TRANSFER, new BigDecimal("20.00"), UUID.randomUUID());

		StepVerifier.create(service().processTransactionEvent(event)).verifyComplete();
		verify(paymentRepository, never()).save(any());
		verify(eventProducer, never()).publishPaymentProcessed(any());
	}

	private Payment paymentWith(PaymentStatus status) {
		Payment payment = new Payment();
		payment.setId(UUID.randomUUID());
		payment.setTransactionId(UUID.randomUUID());
		payment.setAmount(new BigDecimal("10.00"));
		payment.setStatus(status);
		return payment;
	}
}
