package com.clinica.backend.service;

import com.clinica.backend.dto.CreateUserRequest;
import com.clinica.backend.dto.UpdatePasswordRequest;
import com.clinica.backend.dto.UpdateProfileRequest;
import com.clinica.backend.dto.UserProfileDTO;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.exception.BusinessRuleException;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UserProfileDTO createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("El nombre de usuario ya está registrado");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRoles(request.getRoles() != null && !request.getRoles().isEmpty() 
                ? request.getRoles() 
                : Set.of("ROLE_STAFF"));
        user.setSpecialty(request.getSpecialty());
        userRepository.save(user);
        return mapToDTO(user);
    }

    public UserProfileDTO getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return mapToDTO(user);
    }

    public UserProfileDTO updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        userRepository.save(user);
        return mapToDTO(user);
    }

    @Transactional
    public void updatePassword(String username, UpdatePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessRuleException("La contraseña actual es incorrecta");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        user.setEnabled(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,100}$")) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres e incluir mayúsculas, minúsculas, un número y un símbolo");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
    }

    private UserProfileDTO mapToDTO(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roles(user.getRoles())
                .specialty(user.getSpecialty())
                .build();
    }
}
