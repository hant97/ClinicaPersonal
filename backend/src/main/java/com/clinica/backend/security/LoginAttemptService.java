package com.clinica.backend.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCK_DURATION_SECONDS = 15 * 60; // 15 minutos
    public static final long ENTRY_EXPIRATION_SECONDS = 30 * 60; // 30 minutos
    public static final int MAX_CACHE_SIZE = 5000;

    private static class AttemptInfo {
        int attempts;
        Instant lockedUntil;
        Instant lastAttemptTime;

        AttemptInfo(int attempts, Instant lockedUntil) {
            this.attempts = attempts;
            this.lockedUntil = lockedUntil;
            this.lastAttemptTime = Instant.now();
        }
    }

    private final Map<String, AttemptInfo> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key == null) return;
        attemptsCache.remove(key.toLowerCase().trim());
    }

    public void loginFailed(String key) {
        if (key == null) return;
        cleanupIfNecessary();

        String normalizedKey = key.toLowerCase().trim();
        AttemptInfo info = attemptsCache.get(normalizedKey);
        int attempts = (info == null) ? 1 : info.attempts + 1;

        Instant lockedUntil = null;
        if (attempts >= MAX_ATTEMPTS) {
            lockedUntil = Instant.now().plusSeconds(LOCK_DURATION_SECONDS);
        }

        attemptsCache.put(normalizedKey, new AttemptInfo(attempts, lockedUntil));
    }

    public boolean isBlocked(String key) {
        if (key == null) return false;
        String normalizedKey = key.toLowerCase().trim();
        AttemptInfo info = attemptsCache.get(normalizedKey);
        if (info == null || info.lockedUntil == null) {
            return false;
        }

        if (Instant.now().isAfter(info.lockedUntil)) {
            // El bloqueo ya expiró
            attemptsCache.remove(normalizedKey);
            return false;
        }

        return true;
    }

    public long getRemainingLockMinutes(String key) {
        if (key == null) return 0;
        String normalizedKey = key.toLowerCase().trim();
        AttemptInfo info = attemptsCache.get(normalizedKey);
        if (info == null || info.lockedUntil == null) {
            return 0;
        }

        long seconds = info.lockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(1, (seconds + 59) / 60);
    }

    private void cleanupIfNecessary() {
        if (attemptsCache.size() > MAX_CACHE_SIZE / 2) {
            Instant threshold = Instant.now().minusSeconds(ENTRY_EXPIRATION_SECONDS);
            attemptsCache.entrySet().removeIf(entry -> {
                AttemptInfo info = entry.getValue();
                return (info.lockedUntil == null || Instant.now().isAfter(info.lockedUntil))
                        && info.lastAttemptTime.isBefore(threshold);
            });
        }
    }
}
