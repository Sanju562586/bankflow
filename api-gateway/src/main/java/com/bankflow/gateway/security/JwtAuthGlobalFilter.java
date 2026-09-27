package com.bankflow.gateway.security;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.web.cors.reactive.CorsUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGlobalFilter.class);

    private final JwtUtil jwtUtil;

    private static final List<String> OPEN_ENDPOINTS = List.of(
        "/auth",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/fallback"
    );

    public JwtAuthGlobalFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 0. Bypass CORS preflight requests
        if (request.getMethod() == HttpMethod.OPTIONS || CorsUtils.isPreFlightRequest(request)) {
            return chain.filter(exchange);
        }

        // 1. Bypass public endpoints
        for (String openPath : OPEN_ENDPOINTS) {
            if (path.equals(openPath) || path.startsWith(openPath + "/")) {
                return chain.filter(exchange);
            }
        }

        // 2. Check Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or malformed Authorization header. Expected 'Bearer <JWT>'", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.parseAndValidate(token);

            if (jwtUtil.isExpired(claims)) {
                log.warn("Expired JWT token for user {}", claims.getSubject());
                return onError(exchange, "JWT token has expired", HttpStatus.UNAUTHORIZED);
            }

            String username = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            String rolesString = roles != null ? String.join(",", roles) : "ROLE_USER";

            log.debug("Authenticated user={}, roles={} for path={}", username, rolesString, path);

            // 3. Mutate request headers with authenticated identity for downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", username)
                    .header("X-User-Roles", rolesString)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
            return onError(exchange, "Invalid JWT token: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = String.format("{\"error\": \"%s\", \"status\": %d, \"message\": \"%s\"}",
                status.getReasonPhrase(), status.value(), message);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // High precedence before routing and rate limiting
    }
}
