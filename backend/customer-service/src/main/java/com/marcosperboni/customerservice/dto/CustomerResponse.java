package com.marcosperboni.customerservice.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String fullName,
        String documentNumber,
        String email,
        String phone,
        LocalDate birthDate,
        Instant createdAt,
        Instant updatedAt
) {
}
