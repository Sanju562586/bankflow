package com.bankflow.gateway.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "BankflowEnterpriseSecretKeyMustBeAtLeast256BitsLongForHMACSHA256Security";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3600000L); // 1 hour expiration
    }

    @Test
    @DisplayName("generateToken & parseAndValidate: generates valid signed JWT and parses claims")
    void testGenerateAndValidateToken() {
        String token = jwtUtil.generateToken("testuser", List.of("ROLE_CUSTOMER"), Map.of("customerId", "cust-101"));

        assertThat(token).isNotBlank();

        Claims claims = jwtUtil.parseAndValidate(token);
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("customerId")).isEqualTo("cust-101");
        assertThat(jwtUtil.isExpired(claims)).isFalse();
    }
}
