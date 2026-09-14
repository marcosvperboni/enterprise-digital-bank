package com.marcosperboni.apigateway.controller;

import com.marcosperboni.apigateway.dto.LoginRequest;
import com.marcosperboni.apigateway.dto.LoginResponse;
import com.marcosperboni.apigateway.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthControllerTest {

    private final JwtService jwtService = new JwtService("test-secret-key-for-jwt-unit-tests-min-32-bytes", 60);
    private final AuthController controller = new AuthController(jwtService, new BCryptPasswordEncoder());

    @Test
    void loginWithValidAdminCredentialsReturnsTokenWithAdminRole() {
        LoginResponse response = controller.login(new LoginRequest("admin", "admin123")).block();

        assertThat(response).isNotNull();
        assertThat(jwtService.extractUsername(response.token())).isEqualTo("admin");
        assertThat(jwtService.extractRole(response.token())).isEqualTo("ADMIN");
    }

    @Test
    void loginWithValidCustomerCredentialsReturnsTokenWithCustomerRole() {
        LoginResponse response = controller.login(new LoginRequest("customer", "customer123")).block();

        assertThat(response).isNotNull();
        assertThat(jwtService.extractRole(response.token())).isEqualTo("CUSTOMER");
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() {
        assertThatThrownBy(() -> controller.login(new LoginRequest("admin", "wrong-password")).block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void loginWithUnknownUsernameIsUnauthorized() {
        assertThatThrownBy(() -> controller.login(new LoginRequest("ghost", "whatever")).block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }
}
