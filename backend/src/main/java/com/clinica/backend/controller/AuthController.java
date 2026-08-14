package com.clinica.backend.controller;

import com.clinica.backend.dto.AuthRequest;
import com.clinica.backend.dto.AuthResponse;
import com.clinica.backend.repository.UserRepository;
import com.clinica.backend.security.JwtService;
import com.clinica.backend.security.LoginAttemptService;
import com.clinica.backend.security.TokenRevocationService;
import com.clinica.backend.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final RefreshTokenService refreshTokenService;
    private final TokenRevocationService tokenRevocationService;

    @Value("${auth.cookie-secure}")
    private boolean secureCookie;

    @Value("${auth.cookie-same-site:Strict}")
    private String cookieSameSite = "Strict";

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        String username = request.getUsername();
        String ip = extractClientIp(httpRequest);

        if (loginAttemptService.isBlocked(username) || loginAttemptService.isIpBlocked(ip)) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {
            loginAttemptService.loginFailed(username);
            loginAttemptService.loginFailedFromIp(ip);
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        loginAttemptService.loginSucceeded(username);
        loginAttemptService.loginSucceededFromIp(ip);

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));
        var jwtToken = jwtService.generateToken(user);
        setRefreshCookie(httpResponse, refreshTokenService.issue(user));
        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwtToken)
                .specialty(user.getSpecialty())
                .roles(user.getRoles())
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                                HttpServletResponse response) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        var user = rotation.user();
        setRefreshCookie(response, rotation.token());
        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .specialty(user.getSpecialty())
                .roles(user.getRoles())
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                       @RequestHeader(value = "Authorization", required = false) String authorization,
                                       HttpServletResponse response) {
        refreshTokenService.revoke(refreshToken);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            String tokenId = jwtService.extractTokenId(token);
            Date expiration = jwtService.extractClaim(token, claims -> claims.getExpiration());
            if (expiration != null) tokenRevocationService.revoke(tokenId, expiration.toInstant());
        }
        response.addHeader("Set-Cookie", ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(secureCookie).sameSite(cookieSameSite).path("/api/v1/auth").maxAge(0).build().toString());
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String value) {
        response.addHeader("Set-Cookie", ResponseCookie.from("refresh_token", value)
                .httpOnly(true).secure(secureCookie).sameSite(cookieSameSite).path("/api/v1/auth")
                .maxAge(refreshExpiration / 1000).build().toString());
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] parts = xForwardedFor.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("unknown")) {
                    return trimmed;
                }
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !xRealIp.equalsIgnoreCase("unknown")) {
            return xRealIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr.trim() : "127.0.0.1";
    }
}
