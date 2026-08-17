package com.clinica.backend.controller;

import com.clinica.backend.dto.AdminResetPasswordRequest;
import com.clinica.backend.dto.AdminUpdateUserRequest;
import com.clinica.backend.dto.CreateUserRequest;
import com.clinica.backend.dto.UpdatePasswordRequest;
import com.clinica.backend.dto.UpdateProfileRequest;
import com.clinica.backend.dto.UserProfileDTO;
import com.clinica.backend.dto.AuthResponse;
import com.clinica.backend.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfileDTO>> getAllUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllUsers(query, specialty, enabled, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<UserProfileDTO> createUser(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        if (request.getRoles() != null && request.getRoles().contains("ROLE_SITE_ADMIN")
                && !authentication.getAuthorities().stream().anyMatch(a -> "ROLE_SITE_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Solo el administrador principal puede asignar este permiso");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request,
            Authentication authentication) {
        if (request.getRoles() != null && request.getRoles().contains("ROLE_SITE_ADMIN")
                && !authentication.getAuthorities().stream().anyMatch(a -> "ROLE_SITE_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Solo el administrador principal puede asignar este permiso");
        }
        return ResponseEntity.ok(userService.adminUpdateUser(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        userService.toggleUserStatus(id, enabled);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return ResponseEntity.noContent().build();
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

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
