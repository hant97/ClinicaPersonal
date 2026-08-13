package com.clinica.backend.service;

import com.clinica.backend.model.RefreshToken;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository repository;
    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        service = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(service, "expirationMillis", 604_800_000L);
        ReflectionTestUtils.setField(service, "maxActiveTokens", 5);

        user = new User();
        user.setId(1L);
        user.setUsername("admin");
    }

    @Test
    void issuingNewSessionKeepsExistingSessionsBelowPolicyLimit() {
        String firstToken = service.issue(user);
        String secondToken = service.issue(user);

        assertNotEquals(firstToken, secondToken);
        verify(repository, times(2)).save(any(RefreshToken.class));
        verify(repository, times(2)).countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(eq(1L), any());
    }

    @Test
    void rotatingSessionRevokesOnlyPresentedToken() {
        RefreshToken current = new RefreshToken();
        current.setUser(user);
        current.setTokenHash("stored-hash");
        current.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));

        RefreshTokenService.Rotation rotation = service.rotate("current-token");

        assertSame(user, rotation.user());
        assertNotNull(rotation.token());
        assertNotNull(current.getRevokedAt());

        ArgumentCaptor<RefreshToken> savedTokens = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, times(2)).save(savedTokens.capture());
        assertSame(current, savedTokens.getAllValues().get(0));
        assertNotSame(current, savedTokens.getAllValues().get(1));
    }

    @Test
    void retryingARefreshTokenAlreadyConsumedReturnsUnauthorized() {
        RefreshToken current = new RefreshToken();
        current.setUser(user);
        current.setExpiresAt(LocalDateTime.now().plusDays(1));
        current.setRevokedAt(LocalDateTime.now());
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> service.rotate("consumed-token"));
        verify(repository, never()).save(any(RefreshToken.class));
    }

    @Test
    void expiredRefreshTokenReturnsUnauthorized() {
        RefreshToken expired = new RefreshToken();
        expired.setUser(user);
        expired.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(expired));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> service.rotate("expired-token"));
        verify(repository, never()).save(any(RefreshToken.class));
    }

    @Test
    void issuingBeyondSessionLimitRevokesOldestActiveSession() {
        RefreshToken oldest = new RefreshToken();
        oldest.setUser(user);
        oldest.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(repository.countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(eq(1L), any())).thenReturn(5L);
        when(repository.findActiveByUserId(eq(1L), any(), any(Pageable.class))).thenReturn(List.of(oldest));

        service.issue(user);

        assertNotNull(oldest.getRevokedAt());
        verify(repository).saveAll(List.of(oldest));
    }
}
