package com.thrive.portal.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lab 3.4 - rate limiting no login (anti brute force).
 * Limita tentativas por IP nos endpoints de autenticacao. Em producao, use um
 * store distribuido (ex.: Redis) para valer entre instancias.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket bucket(String chave) {
        return buckets.computeIfAbsent(chave, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                .build());
    }

    private boolean ehLogin(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return "POST".equalsIgnoreCase(req.getMethod())
                && ("/login".equals(uri) || "/api/auth/login".equals(uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (ehLogin(request)) {
            String chave = request.getRemoteAddr();
            if (!bucket(chave).tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"erro\":\"muitas tentativas, tente novamente em instantes\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
