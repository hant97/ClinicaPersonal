package com.clinica.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationValidationInterceptorTest {

    private final PaginationValidationInterceptor interceptor = new PaginationValidationInterceptor();

    @Test
    void acceptsZeroBasedPageAndMaximumPageSize() {
        MockHttpServletRequest request = request("0", "100");

        assertDoesNotThrow(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void rejectsNegativePage() {
        MockHttpServletRequest request = request("-1", "20");

        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void rejectsZeroOrOversizedPage() {
        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preHandle(request("0", "0"), new MockHttpServletResponse(), new Object()));
        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preHandle(request("0", "101"), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void rejectsNonNumericParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preHandle(request("first", "20"), new MockHttpServletResponse(), new Object()));
    }

    private MockHttpServletRequest request(String page, String size) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("page", page);
        request.setParameter("size", size);
        return request;
    }
}
