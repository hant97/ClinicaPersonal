package com.clinica.backend.security;

import com.clinica.backend.config.DataInitializer;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void shouldCreateAdminWhenNotExists() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encoded_admin123");

        dataInitializer.run();

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldNotResetPasswordWhenAdminAlreadyExists() throws Exception {
        User existingAdmin = new User();
        existingAdmin.setUsername("admin");
        existingAdmin.setPassword("custom_password_hash");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));

        dataInitializer.run();

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}
