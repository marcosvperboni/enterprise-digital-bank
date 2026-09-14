package com.marcosperboni.apigateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("test-secret-key-for-jwt-unit-tests-min-32-bytes", 60);

    @Test
    void generatesTokenThatValidatesAndReturnsSubjectAndRole() {
        String token = jwtService.generateToken("admin", "ADMIN");

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void rejectsAnExpiredToken() throws InterruptedException {
        JwtService shortLived = new JwtService("test-secret-key-for-jwt-unit-tests-min-32-bytes", 0);
        String token = shortLived.generateToken("admin", "ADMIN");

        Thread.sleep(5);

        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    void rejectsAGarbageToken() {
        assertThat(jwtService.isValid("not-a-real-token")).isFalse();
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-secret-key-32-bytes-min", 60);
        String token = otherService.generateToken("admin", "ADMIN");

        assertThat(jwtService.isValid(token)).isFalse();
    }
}
