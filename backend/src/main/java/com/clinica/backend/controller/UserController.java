package com.clinica.backend.controller;

import com.clinica.backend.dto.CreateUserRequest;
import com.clinica.backend.dto.UpdatePasswordRequest;
import com.clinica.backend.dto.UpdateProfileRequest;
import com.clinica.backend.dto.UserProfileDTO;
import com.clinica.backend.dto.AuthResponse;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.UserRepository;
import com.clinica.backend.security.JwtService;
import com.clinica.backend.security.TokenRevocationService;
import com.clinica.backend.service.RefreshTokenService;
import com.clinica.backend.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenRevocationService tokenRevocationService;

    @Value("${auth.cookie-secure}")
    private boolean secureCookie;

    @Value("${auth.cookie-same-site:Strict}")
    private String cookieSameSite = "Strict";

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<UserProfileDTO> createUser(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        if (request.getRoles() != null && request.getRoles().contains("ROLE_SITE_ADMIN")
                && !authentication.getAuthorities().stream().anyMatch(a -> "ROLE_SITE_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Solo el administrador principal puede asignar este permiso");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userService.getUserProfile(username));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(userService.updateProfile(username, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<AuthResponse> updatePassword(
            Authentication authentication,
            @Valid @RequestBody UpdatePasswordRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletResponse httpResponse) {
        String username = authentication.getName();
        userService.updatePassword(username, request);
        User user = userRepository.findByUsername(username).orElseThrow();

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            String tokenId = jwtService.extractTokenId(token);
            Date expiration = jwtService.extractClaim(token, Claims::getExpiration);
            if (tokenId != null && expiration != null) {
                tokenRevocationService.revoke(tokenId, expiration.toInstant());
            }
        }

        setRefreshCookie(httpResponse, refreshTokenService.issue(user));

        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .specialty(user.getSpecialty())
                .roles(user.getRoles())
                .build());
    }

    private void setRefreshCookie(HttpServletResponse response, String value) {
        response.addHeader("Set-Cookie", ResponseCookie.from("refresh_token", value)
                .httpOnly(true).secure(secureCookie).sameSite(cookieSameSite).path("/api/v1/auth")
                .maxAge(refreshExpiration / 1000).build().toString());
    }
}
