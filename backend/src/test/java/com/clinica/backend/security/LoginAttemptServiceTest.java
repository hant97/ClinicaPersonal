package com.clinica.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    @Test
    void shouldNeverBlockLoopbackIps() {
        String[] loopbacks = {"127.0.0.1", "::1", "0:0:0:0:0:0:0:1", "localhost", null};
        for (String ip : loopbacks) {
            for (int i = 0; i < 50; i++) {
                loginAttemptService.loginFailedFromIp(ip);
            }
            assertFalse(loginAttemptService.isIpBlocked(ip), "Loopback o IP nula no deben bloquearse nunca: " + ip);
        }
    }

    @Test
    void shouldBlockPublicIpAfterMaxIpAttempts() {
        String ip = "198.51.100.42";

        for (int i = 1; i < LoginAttemptService.MAX_IP_ATTEMPTS; i++) {
            loginAttemptService.loginFailedFromIp(ip);
            assertFalse(loginAttemptService.isIpBlocked(ip), "No debe bloquearse antes de alcanzar MAX_IP_ATTEMPTS");
        }

        // Intento 25 (MAX_IP_ATTEMPTS)
        loginAttemptService.loginFailedFromIp(ip);
        assertTrue(loginAttemptService.isIpBlocked(ip), "Debe bloquearse al alcanzar MAX_IP_ATTEMPTS");
    }

    @Test
    void shouldResetIpAttemptsOnSuccessfulLogin() {
        String ip = "203.0.113.15";

        for (int i = 0; i < 10; i++) {
            loginAttemptService.loginFailedFromIp(ip);
        }
        loginAttemptService.loginSucceededFromIp(ip);
        assertFalse(loginAttemptService.isIpBlocked(ip));

        // Otros 24 intentos no deben bloquear
        for (int i = 0; i < LoginAttemptService.MAX_IP_ATTEMPTS - 1; i++) {
            loginAttemptService.loginFailedFromIp(ip);
        }
        assertFalse(loginAttemptService.isIpBlocked(ip));
    }

    @Test
    void shouldCountSimultaneousFailuresWithoutLosingUpdates() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(LoginAttemptService.MAX_ATTEMPTS);
        try {
            for (int round = 0; round < 50; round++) {
                String username = "parallel-" + round;
                CyclicBarrier barrier = new CyclicBarrier(LoginAttemptService.MAX_ATTEMPTS);
                List<Future<?>> failures = new ArrayList<>();
                for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                    failures.add(executor.submit(() -> {
                        barrier.await();
                        loginAttemptService.loginFailed(username);
                        return null;
                    }));
                }
                for (Future<?> failure : failures) {
                    failure.get(5, TimeUnit.SECONDS);
                }
                assertTrue(loginAttemptService.isBlocked(username),
                        "Exactamente MAX_ATTEMPTS fallos simultáneos deben bloquear la cuenta");
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
