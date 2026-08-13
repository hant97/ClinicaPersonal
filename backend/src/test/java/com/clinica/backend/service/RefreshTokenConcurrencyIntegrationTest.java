package com.clinica.backend.service;

import com.clinica.backend.model.User;
import com.clinica.backend.repository.RefreshTokenRepository;
import com.clinica.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenConcurrencyIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void onlyOneConcurrentRefreshCanRotateTheSameToken() throws Exception {
        User user = new User();
        user.setUsername("refresh.concurrent");
        user.setPassword("test-hash");
        user.setSpecialty("PSICOLOGIA");
        user.setRoles(Set.of("ROLE_STAFF"));
        user = userRepository.save(user);
        String originalToken = refreshTokenService.issue(user);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> refresh = () -> {
                ready.countDown();
                start.await();
                try {
                    refreshTokenService.rotate(originalToken);
                    return true;
                } catch (BadCredentialsException exception) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(refresh);
            Future<Boolean> second = executor.submit(refresh);
            ready.await();
            start.countDown();

            assertEquals(1, (first.get() ? 1 : 0) + (second.get() ? 1 : 0));
        } finally {
            executor.shutdownNow();
        }
    }
}
