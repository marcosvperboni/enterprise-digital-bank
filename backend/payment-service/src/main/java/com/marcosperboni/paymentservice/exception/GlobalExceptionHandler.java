package com.marcosperboni.paymentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(PaymentNotFoundException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleNotFound(PaymentNotFoundException ex, ServerWebExchange exchange) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
	}

	@ExceptionHandler(InvalidPaymentTransitionException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleConflict(InvalidPaymentTransitionException ex, ServerWebExchange exchange) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), exchange);
	}

	@ExceptionHandler(WebExchangeBindException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
		String message = ex.getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return build(HttpStatus.BAD_REQUEST, message, exchange);
	}

	@ExceptionHandler(ServerWebInputException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleBadInput(ServerWebInputException ex, ServerWebExchange exchange) {
		return build(HttpStatus.BAD_REQUEST, "Malformed request", exchange);
	}

	private Mono<ResponseEntity<ErrorResponse>> build(HttpStatus status, String message, ServerWebExchange exchange) {
		ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message,
				exchange.getRequest().getPath().value());
		return Mono.just(ResponseEntity.status(status).body(body));
	}
}
