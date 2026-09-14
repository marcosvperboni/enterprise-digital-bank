package com.marcosperboni.customerservice.service;

import com.marcosperboni.customerservice.dto.CustomerRequest;
import com.marcosperboni.customerservice.dto.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    CustomerResponse create(CustomerRequest request);

    Page<CustomerResponse> findAll(Pageable pageable);

    CustomerResponse findById(UUID id);

    CustomerResponse update(UUID id, CustomerRequest request);

    void delete(UUID id);
}
