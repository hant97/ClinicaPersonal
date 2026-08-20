package com.clinica.backend.service;

import com.clinica.backend.dto.AdminUpdateUserRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<UserProfileDTO> getAllUsers(String query, String specialty, Boolean enabled, Pageable pageable) {
        return userRepository.searchUsers(query, specialty, enabled, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return mapToDTO(user);
    }

    @Transactional
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
                : Set.of("ROLE_ADMIN"));
        user.setSpecialty(request.getSpecialty());
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        auditLogService.record(
                "CREATE",
                "USER",
                savedUser.getId().toString(),
                "Usuario creado: " + savedUser.getUsername() + " (Especialidad: " + savedUser.getSpecialty() + ")"
        );

        return mapToDTO(savedUser);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return mapToDTO(user);
    }

    @Transactional
    public UserProfileDTO updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        User updatedUser = userRepository.save(user);

        auditLogService.record(
                "UPDATE",
                "USER",
                updatedUser.getId().toString(),
                "Perfil de usuario actualizado: " + username
        );

        return mapToDTO(updatedUser);
    }

    @Transactional
    public UserProfileDTO adminUpdateUser(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setSpecialty(request.getSpecialty());

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(request.getRoles());
        }

        if (request.getEnabled() != null) {
            boolean wasEnabled = user.isEnabled();
            user.setEnabled(request.getEnabled());
            if (wasEnabled && !request.getEnabled()) {
                user.setTokenVersion(user.getTokenVersion() + 1);
                refreshTokenService.revokeAllForUser(user.getId());
            }
        }

        User updatedUser = userRepository.save(user);

        auditLogService.record(
                "UPDATE",
                "USER",
                updatedUser.getId().toString(),
                "Usuario modificado por administración: " + updatedUser.getUsername()
        );

        return mapToDTO(updatedUser);
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

        auditLogService.record(
                "UPDATE",
                "USER",
                user.getId().toString(),
                "Contraseña modificada por el usuario: " + username
        );
    }

    @Transactional
    public void disableUser(Long userId) {
        toggleUserStatus(userId, false);
    }

    @Transactional
    public void toggleUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
        user.setEnabled(enabled);
        if (!enabled) {
            user.setTokenVersion(user.getTokenVersion() + 1);
            refreshTokenService.revokeAllForUser(userId);
        }
        userRepository.save(user);

        auditLogService.record(
                "UPDATE",
                "USER",
                userId.toString(),
                "Estado de usuario " + user.getUsername() + " cambiado a " + (enabled ? "Habilitado" : "Deshabilitado")
        );
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,100}$")) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres e incluir mayúsculas, minúsculas, un número y un símbolo");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);

        auditLogService.record(
                "UPDATE",
                "USER",
                userId.toString(),
                "Contraseña restablecida por administración para usuario: " + user.getUsername()
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<UserProfileDTO> getProfessionalsInSpecialty(String specialty) {
        return userRepository.findBySpecialtyAndEnabledTrueOrderByFirstNameAscLastNameAsc(specialty)
                .stream()
                .map(this::mapToDTO)
                .collect(java.util.stream.Collectors.toList());
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
                .enabled(user.isEnabled())
                .build();
    }
}
