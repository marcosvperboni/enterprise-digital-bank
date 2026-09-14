package com.marcosperboni.customerservice.controller;

import com.marcosperboni.customerservice.dto.CustomerResponse;
import com.marcosperboni.customerservice.exception.DuplicateResourceException;
import com.marcosperboni.customerservice.exception.ResourceNotFoundException;
import com.marcosperboni.customerservice.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(controllers = CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private static final String VALID_BODY = """
            {"fullName": "Ana Silva", "documentNumber": "12345678901", "email": "ana@example.com", "phone": "11999990000", "birthDate": "1990-01-01"}
            """;

    private static CustomerResponse sampleResponse(UUID id) {
        return new CustomerResponse(id, "Ana Silva", "12345678901", "ana@example.com", "11999990000",
                LocalDate.of(1990, 1, 1), Instant.now(), Instant.now());
    }

    @Test
    void createReturns201WithBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.create(any())).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.fullName").value("Ana Silva"));
    }

    @Test
    void createReturns400OnMissingFields() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createReturns400OnInvalidDocumentNumberFormat() throws Exception {
        String body = """
                {"fullName": "Ana Silva", "documentNumber": "abc", "email": "ana@example.com", "birthDate": "1990-01-01"}
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns409OnDuplicateDocumentNumber() throws Exception {
        when(customerService.create(any())).thenThrow(new DuplicateResourceException("Document number already registered: 12345678901"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void findAllReturnsPagedList() throws Exception {
        UUID id = UUID.randomUUID();
        Page<CustomerResponse> page = new PageImpl<>(List.of(sampleResponse(id)));
        when(customerService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));
    }

    @Test
    void findByIdReturns200WhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.findById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.findById(id)).thenThrow(new ResourceNotFoundException("Customer not found: " + id));

        mockMvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateReturns200WhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.update(eq(id), any())).thenReturn(sampleResponse(id));

        mockMvc.perform(put("/api/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.update(eq(id), any())).thenThrow(new ResourceNotFoundException("Customer not found: " + id));

        mockMvc.perform(put("/api/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturns400OnInvalidBody() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns204WhenFound() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/customers/{id}", id))
                .andExpect(status().isNoContent());

        verify(customerService).delete(id);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Customer not found: " + id))
                .when(customerService).delete(id);

        mockMvc.perform(delete("/api/customers/{id}", id))
                .andExpect(status().isNotFound());
    }
}
