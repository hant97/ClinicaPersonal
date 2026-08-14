package com.clinica.backend.security;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import com.clinica.backend.model.RevokedToken;
import com.clinica.backend.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {
    private final RevokedTokenRepository repository;
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId == null || expiresAt == null) return;
        revokedTokens.put(tokenId, expiresAt);
        if (!repository.existsByTokenId(tokenId)) {
            RevokedToken token = new RevokedToken();
            token.setTokenId(tokenId);
            token.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
            repository.save(token);
        }
    }

    public boolean isRevoked(String tokenId) {
        if (tokenId == null) return false;
        if (repository.existsByTokenId(tokenId)) return true;
        Instant expiry = revokedTokens.get(tokenId);
        if (expiry == null) return false;
        if (expiry.isBefore(Instant.now())) {
            revokedTokens.remove(tokenId);
            return false;
        }
        return true;
    }

    @Scheduled(cron = "${auth.token-cleanup-cron:0 15 * * * *}")
    public void removeExpiredInMemoryTokens() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
