package com.marcosperboni.apigateway.dto;

public record LoginResponse(String token, String tokenType) {

    public LoginResponse(String token) {
        this(token, "Bearer");
    }
}
