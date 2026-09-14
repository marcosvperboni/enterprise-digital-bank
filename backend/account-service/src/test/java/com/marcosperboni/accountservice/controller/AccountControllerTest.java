package com.marcosperboni.accountservice.controller;

import com.marcosperboni.accountservice.domain.AccountStatus;
import com.marcosperboni.accountservice.domain.AccountType;
import com.marcosperboni.accountservice.dto.AccountResponse;
import com.marcosperboni.accountservice.exception.ConflictException;
import com.marcosperboni.accountservice.exception.ResourceNotFoundException;
import com.marcosperboni.accountservice.exception.UnprocessableBalanceException;
import com.marcosperboni.accountservice.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    private static AccountResponse sampleResponse(UUID id, BigDecimal balance, AccountStatus status) {
        return new AccountResponse(id, UUID.randomUUID(), "1234567890", AccountType.CHECKING, balance, status,
                Instant.now(), Instant.now());
    }

    @Test
    void createReturns201WithBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.create(any())).thenReturn(sampleResponse(id, BigDecimal.ZERO, AccountStatus.ACTIVE));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\": \"" + UUID.randomUUID() + "\", \"accountType\": \"CHECKING\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void createReturns400OnMissingFields() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void findAllReturnsPagedList() throws Exception {
        UUID id = UUID.randomUUID();
        Page<AccountResponse> page = new PageImpl<>(List.of(sampleResponse(id, BigDecimal.ZERO, AccountStatus.ACTIVE)));
        when(accountService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));
    }

    @Test
    void findByIdReturns200WhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.findById(id)).thenReturn(sampleResponse(id, BigDecimal.ZERO, AccountStatus.ACTIVE));

        mockMvc.perform(get("/api/accounts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.findById(id)).thenThrow(new ResourceNotFoundException("Account not found: " + id));

        mockMvc.perform(get("/api/accounts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateReturns200WhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.update(eq(id), any())).thenReturn(sampleResponse(id, BigDecimal.ZERO, AccountStatus.BLOCKED));

        mockMvc.perform(put("/api/accounts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountType\": \"CHECKING\", \"status\": \"BLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.update(eq(id), any())).thenThrow(new ResourceNotFoundException("Account not found: " + id));

        mockMvc.perform(put("/api/accounts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountType\": \"CHECKING\", \"status\": \"BLOCKED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturns400OnInvalidBody() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/accounts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns204WhenBalanceIsZero() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/accounts/{id}", id))
                .andExpect(status().isNoContent());

        verify(accountService).delete(id);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Account not found: " + id))
                .when(accountService).delete(id);

        mockMvc.perform(delete("/api/accounts/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns409WhenBalanceIsNotZero() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ConflictException("Cannot close account " + id + " with non-zero balance"))
                .when(accountService).delete(id);

        mockMvc.perform(delete("/api/accounts/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void adjustBalanceReturns200WithUpdatedBalance() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.adjustBalance(eq(id), any())).thenReturn(sampleResponse(id, new BigDecimal("150.00"), AccountStatus.ACTIVE));

        mockMvc.perform(post("/api/accounts/{id}/adjust-balance", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 50.00, \"reason\": \"deposit\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void adjustBalanceReturns422WhenResultWouldBeNegative() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.adjustBalance(eq(id), any()))
                .thenThrow(new UnprocessableBalanceException("Adjustment would result in a negative balance"));

        mockMvc.perform(post("/api/accounts/{id}/adjust-balance", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -500.00, \"reason\": \"withdrawal\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void adjustBalanceReturns400OnMissingFields() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/accounts/{id}/adjust-balance", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
