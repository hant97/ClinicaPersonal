package com.clinica.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PublicRateLimitFilterTest {

    private PublicRateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new PublicRateLimitFilter(new ObjectMapper());
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowRequestsUnderLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/landing");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldBlockWhenPublicRequestsExceedLimit() throws ServletException, IOException {
        String clientIp = "192.168.1.100";

        for (int i = 0; i < 120; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/public/landing");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilterInternal(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        // 121st request should be throttled
        MockHttpServletRequest reqOverLimit = new MockHttpServletRequest("GET", "/api/v1/public/landing");
        reqOverLimit.setRemoteAddr(clientIp);
        MockHttpServletResponse resOverLimit = new MockHttpServletResponse();

        filter.doFilterInternal(reqOverLimit, resOverLimit, filterChain);

        assertEquals(429, resOverLimit.getStatus());
        assertEquals("60", resOverLimit.getHeader("Retry-After"));
    }

    @Test
    void shouldNotBypassLimitByRotatingForwardedForHeader() throws ServletException, IOException {
        String clientIp = "203.0.113.10";

        for (int i = 0; i < 120; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/public/landing");
            req.setRemoteAddr(clientIp);
            req.addHeader("X-Forwarded-For", "198.51.100." + i);
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest reqOverLimit = new MockHttpServletRequest("GET", "/api/v1/public/landing");
        reqOverLimit.setRemoteAddr(clientIp);
        reqOverLimit.addHeader("X-Forwarded-For", "127.0.0.1");
        MockHttpServletResponse resOverLimit = new MockHttpServletResponse();

        filter.doFilterInternal(reqOverLimit, resOverLimit, filterChain);

        assertEquals(429, resOverLimit.getStatus());
    }

    @Test
    void shouldPassNonProtectedRequestsWithoutRateLimiting() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/patients");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
    }
}
