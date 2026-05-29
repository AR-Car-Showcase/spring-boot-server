package com.arcarshowcaseserver.security;

import com.arcarshowcaseserver.configuration.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final RateLimitProperties properties;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || isStaticAsset(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitRule rule = resolveRule(request.getRequestURI());
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = clientKey(request) + ":" + rule.name();
        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, key -> new TokenBucket(
                rule.capacity(properties),
                rule.refillPerMinute(properties)));

        if (!bucket.tryConsume()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"error":"Too Many Requests","message":"%s"}
                    """.formatted(rule.message()));
            response.getWriter().flush();
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isStaticAsset(String uri) {
        return uri.startsWith("/api/models/")
                || uri.startsWith("/api/static/")
                || uri.startsWith("/favicon")
                || uri.startsWith("/_expo")
                || uri.startsWith("/assets");
    }

    private RateLimitRule resolveRule(String uri) {
        if (uri.equals("/login")
                || uri.equals("/login/google")
                || uri.equals("/refresh")
                || uri.equals("/logout")
                || uri.equals("/api/auth/signup")
                || uri.equals("/api/auth/verify-email")
                || uri.equals("/api/auth/resend-verification")) {
            return RateLimitRule.AUTH;
        }

        if (uri.startsWith("/api/customizations")
                || uri.startsWith("/api/cars/ai/")
                || uri.startsWith("/api/cars/recommendations/feedback")) {
            return RateLimitRule.EXPENSIVE;
        }

        if (uri.startsWith("/api/")) {
            return RateLimitRule.GENERAL;
        }

        return null;
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private enum RateLimitRule {
        AUTH {
            @Override
            int capacity(RateLimitProperties properties) {
                return properties.getAuthCapacity();
            }

            @Override
            int refillPerMinute(RateLimitProperties properties) {
                return properties.getAuthRefillPerMinute();
            }

            @Override
            String message() {
                return "Too many authentication requests. Please slow down and try again.";
            }
        },
        EXPENSIVE {
            @Override
            int capacity(RateLimitProperties properties) {
                return properties.getExpensiveCapacity();
            }

            @Override
            int refillPerMinute(RateLimitProperties properties) {
                return properties.getExpensiveRefillPerMinute();
            }

            @Override
            String message() {
                return "Too many expensive requests. Please slow down and try again.";
            }
        },
        GENERAL {
            @Override
            int capacity(RateLimitProperties properties) {
                return properties.getGeneralCapacity();
            }

            @Override
            int refillPerMinute(RateLimitProperties properties) {
                return properties.getGeneralRefillPerMinute();
            }

            @Override
            String message() {
                return "Too many requests. Please slow down and try again.";
            }
        };

        abstract int capacity(RateLimitProperties properties);

        abstract int refillPerMinute(RateLimitProperties properties);

        abstract String message();
    }

    private static final class TokenBucket {
        private final double capacity;
        private final double refillPerNano;
        private double tokens;
        private long lastRefillNanos;

        private TokenBucket(int capacity, int refillPerMinute) {
            this.capacity = Math.max(1, capacity);
            this.refillPerNano = Math.max(1, refillPerMinute) / (double) WINDOW_NANOS;
            this.tokens = this.capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        private synchronized boolean tryConsume() {
            refill();
            if (tokens < 1.0d) {
                return false;
            }
            tokens -= 1.0d;
            return true;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            if (elapsed <= 0) {
                return;
            }

            double refillAmount = elapsed * refillPerNano;
            if (refillAmount <= 0) {
                return;
            }

            tokens = Math.min(capacity, tokens + refillAmount);
            lastRefillNanos = now;
        }
    }
}
