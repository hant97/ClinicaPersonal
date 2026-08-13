package com.clinica.backend.repository;

import com.clinica.backend.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByTokenId(String tokenId);

    long deleteByExpiresAtBefore(LocalDateTime expiredBefore);
}
