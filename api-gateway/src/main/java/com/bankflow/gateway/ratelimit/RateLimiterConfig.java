package com.bankflow.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {

    /**
     * Resolves the rate limit key per user:
     * 1. If 'X-User-Id' header is populated by JWT filter, use user ID.
     * 2. Else if Authorization header is present, use hash of token.
     * 3. Else fallback to client remote IP address.
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return Mono.just("token:" + authHeader.substring(7).hashCode());
            }

            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String hostAddress = (remoteAddress != null && remoteAddress.getAddress() != null)
                    ? remoteAddress.getAddress().getHostAddress()
                    : (remoteAddress != null ? remoteAddress.getHostString() : "127.0.0.1");
            return Mono.just("ip:" + hostAddress);
        };
    }
}
