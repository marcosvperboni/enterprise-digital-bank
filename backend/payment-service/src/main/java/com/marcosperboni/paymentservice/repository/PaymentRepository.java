package com.marcosperboni.paymentservice.repository;

import com.marcosperboni.paymentservice.domain.Payment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface PaymentRepository extends ReactiveCrudRepository<Payment, UUID> {

	Flux<Payment> findAllBy(Pageable pageable);
}
