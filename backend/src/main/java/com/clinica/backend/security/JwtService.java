package com.clinica.backend.security;

import com.clinica.backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String TOKEN_VERSION_CLAIM = "tv";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostConstruct
    public void validateConfiguration() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("La clave secreta JWT (jwt.secret) no está configurada.");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("La clave secreta JWT debe tener al menos 256 bits (32 bytes).");
        }
    }

    /**
     * Verifica firma y expiración una sola vez y devuelve los claims. Vacío si el token es
     * inválido, fue manipulado o expiró.
     */
    public Optional<Claims> parseClaims(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (ExpiredJwtException e) {
            // Caso normal: el frontend renueva el token al recibir 401.
            log.debug("Token JWT expirado");
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido o manipulado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return parseClaims(token).map(claimsResolver).orElse(null);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails.getAuthorities() != null) {
            extraClaims.put("roles", userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList());
        }
        if (userDetails instanceof User user) {
            extraClaims.put("specialty", user.getSpecialty());
        }
        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        if (userDetails instanceof User user) {
            extraClaims.put(TOKEN_VERSION_CLAIM, user.getTokenVersion());
        }
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpiration))
                // Explícito: con una clave de más de 32 bytes jjwt elegiría HS384/HS512.
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    public long extractTokenVersion(String token) {
        return parseClaims(token).map(this::tokenVersion).orElse(-1L);
    }

    public long tokenVersion(Claims claims) {
        return claims.get(TOKEN_VERSION_CLAIM) instanceof Number version ? version.longValue() : -1L;
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return parseClaims(token)
                .map(claims -> userDetails.getUsername().equals(claims.getSubject()))
                .orElse(false);
    }

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
