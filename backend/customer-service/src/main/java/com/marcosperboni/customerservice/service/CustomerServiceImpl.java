package com.marcosperboni.customerservice.service;

import com.marcosperboni.customerservice.domain.Customer;
import com.marcosperboni.customerservice.dto.CustomerRequest;
import com.marcosperboni.customerservice.dto.CustomerResponse;
import com.marcosperboni.customerservice.exception.DuplicateResourceException;
import com.marcosperboni.customerservice.exception.ResourceNotFoundException;
import com.marcosperboni.customerservice.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        assertNotDuplicate(request.documentNumber(), request.email());
        Customer customer = new Customer();
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Override
    public Page<CustomerResponse> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(CustomerServiceImpl::toResponse);
    }

    @Override
    public CustomerResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Override
    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = getOrThrow(id);
        assertNotDuplicateForUpdate(id, request.documentNumber(), request.email());
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer not found: " + id);
        }
        customerRepository.deleteById(id);
    }

    private Customer getOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private void assertNotDuplicate(String documentNumber, String email) {
        if (customerRepository.existsByDocumentNumber(documentNumber)) {
            throw new DuplicateResourceException("Document number already registered: " + documentNumber);
        }
        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }
    }

    private void assertNotDuplicateForUpdate(UUID id, String documentNumber, String email) {
        if (customerRepository.existsByDocumentNumberAndIdNot(documentNumber, id)) {
            throw new DuplicateResourceException("Document number already registered: " + documentNumber);
        }
        if (customerRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }
    }

    private static void applyRequest(Customer customer, CustomerRequest request) {
        customer.setFullName(request.fullName());
        customer.setDocumentNumber(request.documentNumber());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setBirthDate(request.birthDate());
    }

    private static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getDocumentNumber(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getBirthDate(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
