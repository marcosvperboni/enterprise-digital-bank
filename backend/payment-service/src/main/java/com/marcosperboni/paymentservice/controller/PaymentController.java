package com.marcosperboni.paymentservice.controller;

import com.marcosperboni.paymentservice.dto.CreatePaymentRequest;
import com.marcosperboni.paymentservice.dto.PaymentDto;
import com.marcosperboni.paymentservice.dto.UpdatePaymentStatusRequest;
import com.marcosperboni.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<PaymentDto> create(@Valid @RequestBody Mono<CreatePaymentRequest> request) {
		return request.flatMap(paymentService::create);
	}

	@GetMapping
	public Mono<Page<PaymentDto>> findAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return paymentService.findAll(PageRequest.of(page, size));
	}

	@GetMapping("/{id}")
	public Mono<PaymentDto> findById(@PathVariable UUID id) {
		return paymentService.findById(id);
	}

	@PutMapping("/{id}")
	public Mono<PaymentDto> updateStatus(@PathVariable UUID id, @Valid @RequestBody Mono<UpdatePaymentStatusRequest> request) {
		return request.flatMap(body -> paymentService.updateStatus(id, body));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> delete(@PathVariable UUID id) {
		return paymentService.delete(id);
	}
}
