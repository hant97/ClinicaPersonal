package com.clinica.backend.controller;

import com.clinica.backend.config.GlobalExceptionHandler;
import com.clinica.backend.dto.AuthRequest;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.UserRepository;
import com.clinica.backend.security.JwtService;
import com.clinica.backend.security.LoginAttemptService;
import com.clinica.backend.security.TokenRevocationService;
import com.clinica.backend.service.AuditLogService;
import com.clinica.backend.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        String requestJson = "{\"username\":\"admin\",\"password\":\"admin123\"}";

        User user = new User();
        user.setUsername("admin");
        user.setRoles(Set.of("ROLE_ADMIN"));

        when(loginAttemptService.isBlocked("admin")).thenReturn(false);
        when(loginAttemptService.isIpBlocked(anyString())).thenReturn(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("mocked_jwt_token");
        when(refreshTokenService.issue(user)).thenReturn("refresh-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked_jwt_token"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.mustChangePassword").doesNotExist())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Strict")));

        verify(loginAttemptService).loginSucceeded("admin");
    }

    @Test
    void shouldReturn400WhenValidationFailsOnBlankUsername() throws Exception {
        String requestJson = "{\"username\":\"\",\"password\":\"admin123\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    @Test
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        String requestJson = "{\"username\":\"validuser\",\"password\":\"123\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void shouldReturn401WhenBadCredentials() throws Exception {
        String requestJson = "{\"username\":\"admin\",\"password\":\"wrongpassword\"}";

        when(loginAttemptService.isBlocked("admin")).thenReturn(false);
        when(loginAttemptService.isIpBlocked(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));

        verify(loginAttemptService).loginFailed("admin");
    }

    @Test
    void shouldReturn401WhenAccountIsBlocked() throws Exception {
        String requestJson = "{\"username\":\"lockeduser\",\"password\":\"password123\"}";

        when(loginAttemptService.isBlocked("lockeduser")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void shouldReturn401WhenRefreshCookieIsAbsent() throws Exception {
        when(refreshTokenService.rotate(null)).thenThrow(new BadCredentialsException("Refresh token inválido"));

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void logoutRevokesBothRefreshAndAccessTokenAndExpiresCookie() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        when(jwtService.extractTokenId("access-token")).thenReturn("access-id");
        when(jwtService.extractClaim(eq("access-token"), any())).thenReturn(expiration);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refresh_token", "refresh-token"))
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(refreshTokenService).revoke("refresh-token");
        verify(tokenRevocationService).revoke("access-id", expiration.toInstant());
    }
}
