package com.clinica.backend.service;

import com.clinica.backend.dto.CreateUserRequest;
import com.clinica.backend.dto.UserProfileDTO;
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
}
