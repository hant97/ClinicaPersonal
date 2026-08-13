package com.clinica.backend.service;

import com.clinica.backend.model.RefreshToken;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration}")
    private long expirationMillis;

    @Value("${auth.max-active-refresh-tokens}")
    private int maxActiveTokens;

    @Transactional
    public String issue(User user) {
        LocalDateTime now = LocalDateTime.now();
        revokeExcessActiveSessions(user.getId(), now);
        String raw = randomToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(now.plusNanos(expirationMillis * 1_000_000));
        repository.save(token);
        return raw;
    }

    @Transactional
    public Rotation rotate(String rawToken) {
        RefreshToken current = getValidForUpdate(rawToken);
        current.setRevokedAt(LocalDateTime.now());
        repository.save(current);
        return new Rotation(current.getUser(), issue(current.getUser()));
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevokedAt(LocalDateTime.now());
            repository.save(token);
        });
    }

    /** Revoca todas las sesiones de refresh tras cambiar o invalidar credenciales. */
    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllActiveByUserId(userId, LocalDateTime.now());
    }

    private RefreshToken getValidForUpdate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        RefreshToken token = repository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));
        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        return token;
    }

    private void revokeExcessActiveSessions(Long userId, LocalDateTime now) {
        if (maxActiveTokens < 1) {
            throw new IllegalStateException("El límite de sesiones activas debe ser mayor que cero.");
        }
        long activeTokens = repository.countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userId, now);
        int sessionsToRevoke = Math.toIntExact(Math.max(0, activeTokens - maxActiveTokens + 1));
        if (sessionsToRevoke == 0) {
            return;
        }
        List<RefreshToken> excessTokens = repository.findActiveByUserId(
                userId, now, PageRequest.of(0, sessionsToRevoke));
        excessTokens.forEach(token -> token.setRevokedAt(now));
        if (!excessTokens.isEmpty()) {
            repository.saveAll(excessTokens);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    public record Rotation(User user, String token) { }
}
