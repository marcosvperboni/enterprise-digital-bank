package com.marcosperboni.paymentservice.controller;

import com.marcosperboni.paymentservice.domain.PaymentStatus;
import com.marcosperboni.paymentservice.dto.CreatePaymentRequest;
import com.marcosperboni.paymentservice.dto.PaymentDto;
import com.marcosperboni.paymentservice.dto.UpdatePaymentStatusRequest;
import com.marcosperboni.paymentservice.exception.InvalidPaymentTransitionException;
import com.marcosperboni.paymentservice.exception.PaymentNotFoundException;
import com.marcosperboni.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentController.class)
class PaymentControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private PaymentService paymentService;

	@Test
	void create_returns201OnSuccess() {
		UUID transactionId = UUID.randomUUID();
		CreatePaymentRequest request = new CreatePaymentRequest(transactionId, new BigDecimal("100.00"));
		PaymentDto dto = dto(UUID.randomUUID(), transactionId, PaymentStatus.PENDING);
		when(paymentService.create(any(CreatePaymentRequest.class))).thenReturn(Mono.just(dto));

		webTestClient.post().uri("/api/payments")
				.bodyValue(request)
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.status").isEqualTo("PENDING");
	}

	@Test
	void create_returns400WhenAmountIsNotPositive() {
		CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), new BigDecimal("-1.00"));

		webTestClient.post().uri("/api/payments")
				.bodyValue(request)
				.exchange()
				.expectStatus().isBadRequest();
	}

	@Test
	void findAll_returnsPage() {
		PaymentDto dto = dto(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.APPROVED);
		when(paymentService.findAll(any())).thenReturn(Mono.just(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1)));

		webTestClient.get().uri("/api/payments")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.content[0].status").isEqualTo("APPROVED");
	}

	@Test
	void findById_returns200WhenFound() {
		UUID id = UUID.randomUUID();
		PaymentDto dto = dto(id, UUID.randomUUID(), PaymentStatus.PENDING);
		when(paymentService.findById(id)).thenReturn(Mono.just(dto));

		webTestClient.get().uri("/api/payments/{id}", id)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isEqualTo(id.toString());
	}

	@Test
	void findById_returns404WhenMissing() {
		UUID id = UUID.randomUUID();
		when(paymentService.findById(id)).thenReturn(Mono.error(new PaymentNotFoundException(id)));

		webTestClient.get().uri("/api/payments/{id}", id)
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void updateStatus_returns200OnSuccess() {
		UUID id = UUID.randomUUID();
		PaymentDto dto = dto(id, UUID.randomUUID(), PaymentStatus.APPROVED);
		when(paymentService.updateStatus(eq(id), any(UpdatePaymentStatusRequest.class))).thenReturn(Mono.just(dto));

		webTestClient.put().uri("/api/payments/{id}", id)
				.bodyValue(new UpdatePaymentStatusRequest(PaymentStatus.APPROVED))
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("APPROVED");
	}

	@Test
	void updateStatus_returns409OnInvalidTransition() {
		UUID id = UUID.randomUUID();
		when(paymentService.updateStatus(eq(id), any(UpdatePaymentStatusRequest.class)))
				.thenReturn(Mono.error(new InvalidPaymentTransitionException(PaymentStatus.APPROVED, PaymentStatus.REJECTED)));

		webTestClient.put().uri("/api/payments/{id}", id)
				.bodyValue(new UpdatePaymentStatusRequest(PaymentStatus.REJECTED))
				.exchange()
				.expectStatus().isEqualTo(409);
	}

	@Test
	void updateStatus_returns404WhenMissing() {
		UUID id = UUID.randomUUID();
		when(paymentService.updateStatus(eq(id), any(UpdatePaymentStatusRequest.class)))
				.thenReturn(Mono.error(new PaymentNotFoundException(id)));

		webTestClient.put().uri("/api/payments/{id}", id)
				.bodyValue(new UpdatePaymentStatusRequest(PaymentStatus.APPROVED))
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void delete_returns204OnSuccess() {
		UUID id = UUID.randomUUID();
		when(paymentService.delete(id)).thenReturn(Mono.empty());

		webTestClient.delete().uri("/api/payments/{id}", id)
				.exchange()
				.expectStatus().isNoContent();
	}

	@Test
	void delete_returns404WhenMissing() {
		UUID id = UUID.randomUUID();
		when(paymentService.delete(id)).thenReturn(Mono.error(new PaymentNotFoundException(id)));

		webTestClient.delete().uri("/api/payments/{id}", id)
				.exchange()
				.expectStatus().isNotFound();
	}

	private PaymentDto dto(UUID id, UUID transactionId, PaymentStatus status) {
		return new PaymentDto(id, transactionId, new BigDecimal("10.00"), status,
				status == PaymentStatus.PENDING ? null : Instant.now(), Instant.now());
	}
}
