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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	private final PaymentRepository paymentRepository;
	private final PaymentEventProducer eventProducer;

	public PaymentService(PaymentRepository paymentRepository, PaymentEventProducer eventProducer) {
		this.paymentRepository = paymentRepository;
		this.eventProducer = eventProducer;
	}

	public Mono<PaymentDto> create(CreatePaymentRequest request) {
		Payment payment = Payment.newPayment();
		payment.setId(UUID.randomUUID());
		payment.setTransactionId(request.transactionId());
		payment.setAmount(request.amount());
		payment.setStatus(PaymentStatus.PENDING);
		payment.setCreatedAt(Instant.now());
		return paymentRepository.save(payment).map(PaymentMapper::toDto);
	}

	public Mono<Page<PaymentDto>> findAll(Pageable pageable) {
		return paymentRepository.findAllBy(pageable)
				.map(PaymentMapper::toDto)
				.collectList()
				.zipWith(paymentRepository.count())
				.map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
	}

	public Mono<PaymentDto> findById(UUID id) {
		return paymentRepository.findById(id)
				.switchIfEmpty(Mono.error(new PaymentNotFoundException(id)))
				.map(PaymentMapper::toDto);
	}

	public Mono<PaymentDto> updateStatus(UUID id, UpdatePaymentStatusRequest request) {
		return paymentRepository.findById(id)
				.switchIfEmpty(Mono.error(new PaymentNotFoundException(id)))
				.flatMap(payment -> {
					validateTransition(payment.getStatus(), request.status());
					payment.setStatus(request.status());
					payment.setProcessedAt(Instant.now());
					return paymentRepository.save(payment);
				})
				.map(PaymentMapper::toDto);
	}

	public Mono<Void> delete(UUID id) {
		return paymentRepository.findById(id)
				.switchIfEmpty(Mono.error(new PaymentNotFoundException(id)))
				.flatMap(paymentRepository::delete);
	}

	public Mono<Void> processTransactionEvent(TransactionCreatedEvent event) {
		if (event.type() != TransactionType.DEPOSIT && event.type() != TransactionType.WITHDRAWAL) {
			log.info("Ignoring transaction {} of type {}: payment-service only auto-processes DEPOSIT/WITHDRAWAL",
					event.transactionId(), event.type());
			return Mono.empty();
		}

		Payment payment = Payment.newPayment();
		payment.setId(UUID.randomUUID());
		payment.setTransactionId(event.transactionId());
		payment.setAmount(event.amount());
		payment.setStatus(PaymentStatus.PENDING);
		payment.setCreatedAt(Instant.now());

		return paymentRepository.save(payment)
				.flatMap(saved -> {
					saved.markPersisted();
					saved.setStatus(PaymentStatus.APPROVED);
					saved.setProcessedAt(Instant.now());
					return paymentRepository.save(saved);
				})
				.doOnNext(saved -> log.info("Payment {} for transaction {} auto-approved", saved.getId(), saved.getTransactionId()))
				.flatMap(saved -> eventProducer.publishPaymentProcessed(
						new PaymentProcessedEvent(saved.getId(), saved.getTransactionId(), saved.getStatus())));
	}

	private void validateTransition(PaymentStatus current, PaymentStatus target) {
		boolean validTarget = target == PaymentStatus.APPROVED || target == PaymentStatus.REJECTED;
		if (current != PaymentStatus.PENDING || !validTarget) {
			throw new InvalidPaymentTransitionException(current, target);
		}
	}
}
