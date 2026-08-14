package com.clinica.backend.security;

import com.clinica.backend.dto.ApiErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final int PUBLIC_MAX_REQUESTS_PER_MINUTE = 120;
    private static final int REFRESH_MAX_REQUESTS_PER_MINUTE = 30;
    private static final long WINDOW_DURATION_MILLIS = 60_000L;

    private final ObjectMapper objectMapper;
    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isPublic = path != null && path.startsWith("/api/v1/public/");
        boolean isRefresh = path != null && path.equals("/api/v1/auth/refresh");

        if (!isPublic && !isRefresh) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        String counterKey = (isPublic ? "PUB:" : "REF:") + clientIp;
        int maxAllowed = isPublic ? PUBLIC_MAX_REQUESTS_PER_MINUTE : REFRESH_MAX_REQUESTS_PER_MINUTE;

        long now = System.currentTimeMillis();
        RequestCounter counter = counters.compute(counterKey, (key, existing) -> {
            if (existing == null || (now - existing.windowStartMillis) > WINDOW_DURATION_MILLIS) {
                return new RequestCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter != null && counter.count.get() > maxAllowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                    .message("Demasiadas solicitudes. Por favor, espere un momento antes de reintentar.")
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            String candidate = parts[0].trim();
            if (!candidate.equalsIgnoreCase("unknown") && !candidate.isBlank()) {
                return candidate;
            }
        }
        return request.getRemoteAddr();
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanupStaleCounters() {
        long now = System.currentTimeMillis();
        counters.entrySet().removeIf(entry -> (now - entry.getValue().windowStartMillis) > (WINDOW_DURATION_MILLIS * 2));
    }

    public static class RequestCounter {
        final long windowStartMillis;
        final AtomicInteger count;

        public RequestCounter(long windowStartMillis, AtomicInteger count) {
            this.windowStartMillis = windowStartMillis;
            this.count = count;
        }
    }
}
