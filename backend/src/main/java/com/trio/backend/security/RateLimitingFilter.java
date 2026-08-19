package com.trio.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trio.backend.common.ApiResponse;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Bucket globalRateLimiter;
    private final Bucket aiRateLimiter;
    private final Bucket authRateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        Bucket bucket = selectBucket(path);

        if (bucket != null && !bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for {} {}", method, path);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            objectMapper.writeValue(
                    response.getOutputStream(),
                    ApiResponse.failure("Too many requests. Please try again later.")
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Bucket selectBucket(String path) {
        if (path.startsWith("/api/auth/") && !path.startsWith("/api/auth/me")) {
            return authRateLimiter;
        }
        if (path.contains("/ai/") || path.contains("/analytics/ai") || path.contains("/knowledge/ai")
                || path.contains("/handover/ai") || path.contains("/reporting/ai")) {
            return aiRateLimiter;
        }
        return globalRateLimiter;
    }
}
