package com.marcosperboni.transactionservice.controller;

import com.marcosperboni.transactionservice.dto.CreateTransactionRequest;
import com.marcosperboni.transactionservice.dto.TransactionDto;
import com.marcosperboni.transactionservice.dto.UpdateTransactionRequest;
import com.marcosperboni.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TransactionDto create(@Valid @RequestBody CreateTransactionRequest request) {
		return transactionService.create(request);
	}

	@GetMapping
	public Page<TransactionDto> findAll(@RequestParam(required = false) UUID accountId, Pageable pageable) {
		return transactionService.findAll(accountId, pageable);
	}

	@GetMapping("/{id}")
	public TransactionDto findById(@PathVariable UUID id) {
		return transactionService.findById(id);
	}

	@PutMapping("/{id}")
	public TransactionDto update(@PathVariable UUID id, @RequestBody UpdateTransactionRequest request) {
		return transactionService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		transactionService.delete(id);
	}
}
