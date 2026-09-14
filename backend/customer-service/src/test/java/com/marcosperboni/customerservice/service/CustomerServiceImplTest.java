package com.marcosperboni.customerservice.service;

import com.marcosperboni.customerservice.domain.Customer;
import com.marcosperboni.customerservice.dto.CustomerRequest;
import com.marcosperboni.customerservice.dto.CustomerResponse;
import com.marcosperboni.customerservice.exception.DuplicateResourceException;
import com.marcosperboni.customerservice.exception.ResourceNotFoundException;
import com.marcosperboni.customerservice.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerServiceImpl service() {
        return new CustomerServiceImpl(customerRepository);
    }

    private static CustomerRequest validRequest() {
        return new CustomerRequest("Ana Silva", "12345678901", "ana@example.com", "11999990000", LocalDate.of(1990, 1, 1));
    }

    @Test
    void createSavesAndReturnsResponseWhenNoDuplicate() {
        when(customerRepository.existsByDocumentNumber("12345678901")).thenReturn(false);
        when(customerRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CustomerResponse response = service().create(validRequest());

        assertThat(response.id()).isNotNull();
        assertThat(response.fullName()).isEqualTo("Ana Silva");
        assertThat(response.documentNumber()).isEqualTo("12345678901");
    }

    @Test
    void createThrowsDuplicateWhenDocumentNumberAlreadyRegistered() {
        when(customerRepository.existsByDocumentNumber("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> service().create(validRequest()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void createThrowsDuplicateWhenEmailAlreadyRegistered() {
        when(customerRepository.existsByDocumentNumber("12345678901")).thenReturn(false);
        when(customerRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service().create(validRequest()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsResponseWhenCustomerExists() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName("Ana");
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        CustomerResponse response = service().findById(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void findByIdThrowsNotFoundWhenCustomerMissing() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChangesWhenCustomerExists() {
        UUID id = UUID.randomUUID();
        Customer existing = new Customer();
        existing.setId(id);
        existing.setDocumentNumber("00000000000");
        existing.setEmail("old@example.com");
        when(customerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByDocumentNumberAndIdNot("12345678901", id)).thenReturn(false);
        when(customerRepository.existsByEmailAndIdNot("ana@example.com", id)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = service().update(id, validRequest());

        assertThat(response.fullName()).isEqualTo("Ana Silva");
        assertThat(response.email()).isEqualTo("ana@example.com");
    }

    @Test
    void updateThrowsNotFoundWhenCustomerMissing() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(id, validRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesCustomerWhenExists() {
        UUID id = UUID.randomUUID();
        when(customerRepository.existsById(id)).thenReturn(true);

        service().delete(id);

        verify(customerRepository).deleteById(id);
    }

    @Test
    void deleteThrowsNotFoundWhenCustomerMissing() {
        UUID id = UUID.randomUUID();
        when(customerRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service().delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(customerRepository, never()).deleteById(any());
    }
}
