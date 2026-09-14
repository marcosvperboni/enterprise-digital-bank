package com.marcosperboni.customerservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CustomerRequest(
        @NotBlank String fullName,
        // ponytail: format-only check (11 digits), no checksum validation; add the mod-11 CPF algorithm if fraud checks matter later.
        @NotBlank @Pattern(regexp = "\\d{11}", message = "must be 11 digits") String documentNumber,
        @NotBlank @Email String email,
        String phone,
        @NotNull @Past LocalDate birthDate
) {
}
