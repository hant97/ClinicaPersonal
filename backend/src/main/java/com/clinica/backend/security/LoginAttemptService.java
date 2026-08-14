package com.clinica.backend.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int MAX_IP_ATTEMPTS = 25;
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
    private final Map<String, AttemptInfo> ipAttemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key == null) return;
        attemptsCache.remove(key.toLowerCase().trim());
    }

    public void loginSucceededFromIp(String ip) {
        if (ip == null || isLoopbackOrLocal(ip)) return;
        ipAttemptsCache.remove(ip.trim());
    }

    public void loginFailed(String key) {
        if (key == null) return;
        cleanupIfNecessary(attemptsCache);

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

    public boolean isIpBlocked(String ip) {
        if (ip == null || isLoopbackOrLocal(ip)) return false;
        return isBlockedFrom(ipAttemptsCache, ip);
    }

    public void loginFailedFromIp(String ip) {
        if (ip == null || isLoopbackOrLocal(ip)) return;
        cleanupIfNecessary(ipAttemptsCache);
        recordIpFailure(ipAttemptsCache, ip);
    }

    private boolean isLoopbackOrLocal(String ip) {
        if (ip == null) return true;
        String trimmed = ip.trim();
        return trimmed.equals("127.0.0.1") || trimmed.equals("0:0:0:0:0:0:0:1") || trimmed.equals("::1") || trimmed.equalsIgnoreCase("localhost");
    }

    private boolean isBlockedFrom(Map<String, AttemptInfo> cache, String key) {
        if (key == null) return false;
        String normalizedKey = key.trim();
        AttemptInfo info = cache.get(normalizedKey);
        if (info == null || info.lockedUntil == null) return false;
        if (Instant.now().isAfter(info.lockedUntil)) {
            cache.remove(normalizedKey);
            return false;
        }
        return true;
    }

    private void recordIpFailure(Map<String, AttemptInfo> cache, String key) {
        if (key == null || key.isBlank()) return;
        String normalizedKey = key.trim();
        AttemptInfo info = cache.get(normalizedKey);
        int attempts = info == null ? 1 : info.attempts + 1;
        Instant lockedUntil = attempts >= MAX_IP_ATTEMPTS
                ? Instant.now().plusSeconds(LOCK_DURATION_SECONDS) : null;
        cache.put(normalizedKey, new AttemptInfo(attempts, lockedUntil));
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

    private void cleanupIfNecessary(Map<String, AttemptInfo> cache) {
        if (cache.size() > MAX_CACHE_SIZE / 2) {
            Instant threshold = Instant.now().minusSeconds(ENTRY_EXPIRATION_SECONDS);
            cache.entrySet().removeIf(entry -> {
                AttemptInfo info = entry.getValue();
                return (info.lockedUntil == null || Instant.now().isAfter(info.lockedUntil))
                        && info.lastAttemptTime.isBefore(threshold);
            });
        }
    }
}
