package com.marcosperboni.apigateway.controller;

import com.marcosperboni.apigateway.dto.LoginRequest;
import com.marcosperboni.apigateway.dto.LoginResponse;
import com.marcosperboni.apigateway.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Demo-only in-memory user store; passwords encoded once at class load so plaintext never sits in a field.
    private record DemoUser(String username, String passwordHash, String role) {
    }

    private static final PasswordEncoder BOOTSTRAP_ENCODER = new BCryptPasswordEncoder();
    private static final List<DemoUser> USERS = List.of(
            new DemoUser("admin", BOOTSTRAP_ENCODER.encode("admin123"), "ADMIN"),
            new DemoUser("customer", BOOTSTRAP_ENCODER.encode("customer123"), "CUSTOMER"));

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return USERS.stream()
                .filter(user -> user.username().equals(request.username()))
                .filter(user -> passwordEncoder.matches(request.password(), user.passwordHash()))
                .findFirst()
                .map(user -> Mono.just(new LoginResponse(jwtService.generateToken(user.username(), user.role()))))
                .orElseGet(() -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")));
    }
}
