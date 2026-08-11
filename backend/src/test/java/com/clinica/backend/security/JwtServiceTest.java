package com.clinica.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String validSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expiration = 86400000; // 24h

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", validSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", expiration);
        jwtService.validateConfiguration();
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully() {
        UserDetails user = new User("admin", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String token = jwtService.generateToken(user);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("admin", username);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void shouldFailValidationWhenUsernameMismatch() {
        UserDetails user = new User("admin", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UserDetails differentUser = new User("otherUser", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtService.generateToken(user);
        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    void shouldFailValidationWhenTokenIsTampered() {
        UserDetails user = new User("admin", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        String token = jwtService.generateToken(user);

        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";
        assertFalse(jwtService.isTokenValid(tamperedToken, user));
    }
}
