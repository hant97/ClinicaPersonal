package com.clinica.backend.service;

import com.clinica.backend.repository.RefreshTokenRepository;
import com.clinica.backend.repository.RevokedTokenRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TokenCleanupServiceTest {

    @Test
    void removesExpiredRefreshAndAccessTokenRevocations() {
        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        RevokedTokenRepository revokedTokens = mock(RevokedTokenRepository.class);

        new TokenCleanupService(refreshTokens, revokedTokens).removeExpiredTokens();

        verify(refreshTokens).deleteByExpiresAtBefore(any());
        verify(revokedTokens).deleteByExpiresAtBefore(any());
    }
}
