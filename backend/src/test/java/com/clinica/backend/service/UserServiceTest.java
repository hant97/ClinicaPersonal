package com.clinica.backend.service;

import com.clinica.backend.dto.CreateUserRequest;
import com.clinica.backend.dto.UserProfileDTO;
import com.clinica.backend.dto.UpdatePasswordRequest;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUserSuccessfully() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("doctor.test")
                .password("plain_pass")
                .firstName("Test")
                .lastName("Doctor")
                .email("test@clinica.com")
                .phone("999888777")
                .roles(Set.of("ROLE_DOCTOR"))
                .build();

        when(userRepository.findByUsername("doctor.test")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain_pass")).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        UserProfileDTO result = userService.createUser(request);

        assertNotNull(result);
        assertEquals("doctor.test", result.getUsername());
        verify(passwordEncoder).encode("plain_pass");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("existing.user")
                .password("plain_pass")
                .build();

        when(userRepository.findByUsername("existing.user")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changingPasswordInvalidatesAllPreviousSessionsAndAccessTokens() {
        User user = new User();
        user.setId(10L);
        user.setUsername("doctor.test");
        user.setPassword("old-hash");
        user.setTokenVersion(2L);
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .currentPassword("Current1!")
                .newPassword("Replacement1!")
                .build();
        when(userRepository.findByUsername("doctor.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current1!", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("Replacement1!")).thenReturn("new-hash");

        userService.updatePassword("doctor.test", request);

        assertEquals(3L, user.getTokenVersion());
        assertEquals("new-hash", user.getPassword());
        verify(refreshTokenService).revokeAllForUser(10L);
    }

    @Test
    void disablingOrResettingCredentialsInvalidatesAllRefreshSessions() {
        User user = new User();
        user.setId(10L);
        user.setTokenVersion(4L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Temporary1!")).thenReturn("temporary-hash");

        userService.disableUser(10L);
        userService.resetPassword(10L, "Temporary1!");

        assertFalse(user.isEnabled());
        assertEquals(6L, user.getTokenVersion());
        verify(refreshTokenService, times(2)).revokeAllForUser(10L);
    }
}
