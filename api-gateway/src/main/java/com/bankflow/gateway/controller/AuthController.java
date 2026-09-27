package com.bankflow.gateway.controller;

import com.bankflow.gateway.dto.AuthRequest;
import com.bankflow.gateway.dto.AuthResponse;
import com.bankflow.gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Issues a signed JWT token with user credentials and roles.
     * Default test accounts:
     * - admin / admin123 -> ROLE_ADMIN
     * - customer1 / pass123 -> ROLE_CUSTOMER
     * - operator / pass123 -> ROLE_OPERATOR
     */
    @PostMapping("/token")
    public Mono<ResponseEntity<AuthResponse>> generateToken(@RequestBody AuthRequest request) {
        String username = request.getUsername() != null && !request.getUsername().isBlank() 
                ? request.getUsername() 
                : "customer-" + UUID.randomUUID().toString().substring(0, 6);

        List<String> roles = new ArrayList<>();
        if ("admin".equalsIgnoreCase(username)) {
            roles.add("ROLE_ADMIN");
            roles.add("ROLE_OPERATOR");
        } else if ("operator".equalsIgnoreCase(username)) {
            roles.add("ROLE_OPERATOR");
        } else {
            roles.add(request.getRole() != null ? request.getRole() : "ROLE_CUSTOMER");
        }

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("iss", "bankflow-gateway");
        extraClaims.put("customerId", "cust-" + Math.abs(username.hashCode() % 10000));

        String token = jwtUtil.generateToken(username, roles, extraClaims);
        AuthResponse response = new AuthResponse(token, username, roles, 86400000L);

        return Mono.just(ResponseEntity.ok(response));
    }

    /**
     * Validates an existing token.
     */
    @PostMapping("/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validateToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        Map<String, Object> result = new HashMap<>();

        if (token == null || token.isBlank()) {
            result.put("valid", false);
            result.put("error", "Token is required");
            return Mono.just(ResponseEntity.badRequest().body(result));
        }

        try {
            Claims claims = jwtUtil.parseAndValidate(token);
            result.put("valid", !jwtUtil.isExpired(claims));
            result.put("username", claims.getSubject());
            result.put("roles", claims.get("roles"));
            result.put("expiration", claims.getExpiration().toString());
            return Mono.just(ResponseEntity.ok(result));
        } catch (Exception ex) {
            result.put("valid", false);
            result.put("error", ex.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result));
        }
    }
}
