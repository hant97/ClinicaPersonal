package com.clinica.backend.service;

import com.clinica.backend.repository.RefreshTokenRepository;
import com.clinica.backend.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;

    @Scheduled(cron = "${auth.token-cleanup-cron:0 15 * * * *}")
    @Transactional
    public void removeExpiredTokens() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        refreshTokenRepository.deleteByExpiresAtBefore(now);
        revokedTokenRepository.deleteByExpiresAtBefore(now);
    }
}
