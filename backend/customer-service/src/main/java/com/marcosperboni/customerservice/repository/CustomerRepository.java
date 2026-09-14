package com.marcosperboni.customerservice.repository;

import com.marcosperboni.customerservice.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumberAndIdNot(String documentNumber, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
