package com.clinica.backend.security;

import com.clinica.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

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
        User user = createUser("admin", "ROLE_ADMIN");

        String token = jwtService.generateToken(user);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("admin", username);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void shouldFailValidationWhenUsernameMismatch() {
        User user = createUser("admin", "ROLE_ADMIN");
        User differentUser = createUser("otherUser", "ROLE_USER");

        String token = jwtService.generateToken(user);
        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    void shouldFailValidationWhenTokenIsMalformed() {
        User user = createUser("admin", "ROLE_ADMIN");
        assertFalse(jwtService.isTokenValid("malformed.token.value", user));
    }

    @Test
    void shouldFailValidationWhenTokenIsTampered() {
        User user = createUser("admin", "ROLE_ADMIN");
        String token = jwtService.generateToken(user);

        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";
        assertFalse(jwtService.isTokenValid(tamperedToken, user));
    }

    @Test
    void shouldFailSafelyWhenJwtSecretIsMissing() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "");

        assertThrows(IllegalStateException.class, jwtService::validateConfiguration);
    }

    @Test
    void shouldGenerateAndExtractTokenVersion() {
        User user = createUser("doctor", "ROLE_ADMIN");
        user.setTokenVersion(3);

        String token = jwtService.generateToken(user);
        assertNotNull(token);

        long version = jwtService.extractTokenVersion(token);
        assertEquals(3L, version);
        assertNotNull(jwtService.extractTokenId(token));
    }

    @Test
    void shouldKeepHs256EvenWithKeysLongerThan32Bytes() {
        // validSecret decodifica a 48 bytes: sin algoritmo explícito jjwt firmaría con HS384.
        String token = jwtService.generateToken(createUser("admin", "ROLE_ADMIN"));
        String header = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]));

        assertTrue(header.contains("\"alg\":\"HS256\""), header);
    }

    @Test
    void shouldRejectExpiredTokens() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        User user = createUser("admin", "ROLE_ADMIN");
        String expired = jwtService.generateToken(user);

        assertTrue(jwtService.parseClaims(expired).isEmpty());
        assertFalse(jwtService.isTokenValid(expired, user));
        assertEquals(-1L, jwtService.extractTokenVersion(expired));
    }

    private User createUser(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setRoles(Set.of(role));
        user.setSpecialty("PSICOLOGIA");
        return user;
    }
}
