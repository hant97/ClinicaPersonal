package com.clinica.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void shouldIgnoreClientSuppliedForwardingHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "127.0.0.1");
        request.addHeader("X-Real-IP", "127.0.0.1");

        assertEquals("203.0.113.10", ClientIpResolver.resolve(request));
    }

    @Test
    void shouldReturnUnknownWhenRemoteAddressIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("  ");

        assertEquals(ClientIpResolver.UNKNOWN, ClientIpResolver.resolve(request));
        assertEquals(ClientIpResolver.UNKNOWN, ClientIpResolver.resolve(null));
    }
}
