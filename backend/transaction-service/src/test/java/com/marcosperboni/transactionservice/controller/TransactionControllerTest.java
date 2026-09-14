package com.marcosperboni.transactionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcosperboni.transactionservice.domain.TransactionStatus;
import com.marcosperboni.transactionservice.domain.TransactionType;
import com.marcosperboni.transactionservice.dto.CreateTransactionRequest;
import com.marcosperboni.transactionservice.dto.TransactionDto;
import com.marcosperboni.transactionservice.dto.UpdateTransactionRequest;
import com.marcosperboni.transactionservice.exception.InvalidTransactionException;
import com.marcosperboni.transactionservice.exception.TransactionNotDeletableException;
import com.marcosperboni.transactionservice.exception.TransactionNotFoundException;
import com.marcosperboni.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private TransactionService transactionService;

	@Test
	void create_returns201OnSuccess() throws Exception {
		UUID accountId = UUID.randomUUID();
		CreateTransactionRequest request = new CreateTransactionRequest(
				accountId, TransactionType.DEPOSIT, new BigDecimal("100.00"), "BRL", null, "deposit");
		TransactionDto dto = new TransactionDto(UUID.randomUUID(), accountId, TransactionType.DEPOSIT,
				new BigDecimal("100.00"), "BRL", null, TransactionStatus.PENDING, "deposit", Instant.now());
		when(transactionService.create(any(CreateTransactionRequest.class))).thenReturn(dto);

		mockMvc.perform(post("/api/transactions")
						.contentType("application/json")
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	void create_returns400WhenAmountIsNotPositive() throws Exception {
		CreateTransactionRequest request = new CreateTransactionRequest(
				UUID.randomUUID(), TransactionType.DEPOSIT, new BigDecimal("-1.00"), "BRL", null, null);

		mockMvc.perform(post("/api/transactions")
						.contentType("application/json")
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_returns400WhenBusinessValidationFails() throws Exception {
		CreateTransactionRequest request = new CreateTransactionRequest(
				UUID.randomUUID(), TransactionType.TRANSFER, new BigDecimal("10.00"), "BRL", null, null);
		when(transactionService.create(any(CreateTransactionRequest.class)))
				.thenThrow(new InvalidTransactionException("destinationAccountId is required for TRANSFER transactions"));

		mockMvc.perform(post("/api/transactions")
						.contentType("application/json")
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void findAll_returnsPage() throws Exception {
		TransactionDto dto = new TransactionDto(UUID.randomUUID(), UUID.randomUUID(), TransactionType.WITHDRAWAL,
				BigDecimal.TEN, "BRL", null, TransactionStatus.PENDING, null, Instant.now());
		when(transactionService.findAll(isNull(), any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/transactions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].type").value("WITHDRAWAL"));
	}

	@Test
	void findById_returns200WhenFound() throws Exception {
		UUID id = UUID.randomUUID();
		TransactionDto dto = new TransactionDto(id, UUID.randomUUID(), TransactionType.DEPOSIT,
				BigDecimal.TEN, "BRL", null, TransactionStatus.PENDING, null, Instant.now());
		when(transactionService.findById(id)).thenReturn(dto);

		mockMvc.perform(get("/api/transactions/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()));
	}

	@Test
	void findById_returns404WhenMissing() throws Exception {
		UUID id = UUID.randomUUID();
		when(transactionService.findById(id)).thenThrow(new TransactionNotFoundException(id));

		mockMvc.perform(get("/api/transactions/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void update_returns200OnSuccess() throws Exception {
		UUID id = UUID.randomUUID();
		UpdateTransactionRequest request = new UpdateTransactionRequest("new desc", TransactionStatus.COMPLETED);
		TransactionDto dto = new TransactionDto(id, UUID.randomUUID(), TransactionType.DEPOSIT,
				BigDecimal.TEN, "BRL", null, TransactionStatus.COMPLETED, "new desc", Instant.now());
		when(transactionService.update(eq(id), any(UpdateTransactionRequest.class))).thenReturn(dto);

		mockMvc.perform(put("/api/transactions/{id}", id)
						.contentType("application/json")
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void update_returns404WhenMissing() throws Exception {
		UUID id = UUID.randomUUID();
		UpdateTransactionRequest request = new UpdateTransactionRequest("x", null);
		when(transactionService.update(eq(id), any(UpdateTransactionRequest.class)))
				.thenThrow(new TransactionNotFoundException(id));

		mockMvc.perform(put("/api/transactions/{id}", id)
						.contentType("application/json")
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

	@Test
	void delete_returns204OnSuccess() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(delete("/api/transactions/{id}", id))
				.andExpect(status().isNoContent());
	}

	@Test
	void delete_returns409WhenNotPending() throws Exception {
		UUID id = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new TransactionNotDeletableException(id, TransactionStatus.COMPLETED))
				.when(transactionService).delete(id);

		mockMvc.perform(delete("/api/transactions/{id}", id))
				.andExpect(status().isConflict());
	}

	@Test
	void delete_returns404WhenMissing() throws Exception {
		UUID id = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new TransactionNotFoundException(id))
				.when(transactionService).delete(id);

		mockMvc.perform(delete("/api/transactions/{id}", id))
				.andExpect(status().isNotFound());
	}
}
