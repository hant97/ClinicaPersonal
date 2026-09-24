package com.clinica.backend.security;

import com.clinica.backend.model.User;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private TokenRevocationService tokenRevocationService;
    private FilterChain chain;
    private JwtAuthenticationFilter filter;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "dGVzdC1vbmx5LWp3dC1zaWduaW5nLWtleS0zMi1ieXRlcy0xMjM0NTY=");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
        userDetailsService = mock(UserDetailsService.class);
        tokenRevocationService = mock(TokenRevocationService.class);
        chain = mock(FilterChain.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, tokenRevocationService);

        user = new User();
        user.setUsername("doctora");
        user.setPassword("x");
        user.setRoles(Set.of("ROLE_ADMIN"));
        user.setSpecialty("PSICOLOGIA");
        when(userDetailsService.loadUserByUsername("doctora")).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidToken() throws Exception {
        run(jwtService.generateToken(user));

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsTokenWithOutdatedVersion() throws Exception {
        String token = jwtService.generateToken(user);
        user.setTokenVersion(user.getTokenVersion() + 1);

        run(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsDisabledUser() throws Exception {
        String token = jwtService.generateToken(user);
        user.setEnabled(false);

        run(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsRevokedToken() throws Exception {
        String token = jwtService.generateToken(user);
        when(tokenRevocationService.isRevoked(any())).thenReturn(true);

        run(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidTokenContinuesUnauthenticatedWithoutLoadingTheUser() throws Exception {
        run("no.es.un-token");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void tokenOfDeletedUserContinuesUnauthenticated() throws Exception {
        String token = jwtService.generateToken(user);
        when(userDetailsService.loadUserByUsername("doctora")).thenThrow(new UsernameNotFoundException("x"));

        run(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private void run(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/patients");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
