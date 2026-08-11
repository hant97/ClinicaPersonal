package com.clinica.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Test
    void shouldNotBeBlockedInitially() {
        assertFalse(loginAttemptService.isBlocked("admin"));
        assertEquals(0, loginAttemptService.getRemainingLockMinutes("admin"));
    }

    @Test
    void shouldBlockAfterMaxAttempts() {
        String username = "attacker";

        for (int i = 1; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            loginAttemptService.loginFailed(username);
            assertFalse(loginAttemptService.isBlocked(username), "No debe bloquearse antes de alcanzar MAX_ATTEMPTS");
        }

        // Intento número 5 (MAX_ATTEMPTS)
        loginAttemptService.loginFailed(username);
        assertTrue(loginAttemptService.isBlocked(username), "Debe bloquearse al alcanzar MAX_ATTEMPTS");
        assertTrue(loginAttemptService.getRemainingLockMinutes(username) > 0);
    }

    @Test
    void shouldResetAttemptsOnSuccessfulLogin() {
        String username = "user1";

        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginSucceeded(username);

        assertFalse(loginAttemptService.isBlocked(username));

        // Otros 4 intentos no deben bloquear aún
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            loginAttemptService.loginFailed(username);
        }
        assertFalse(loginAttemptService.isBlocked(username));
    }
}
