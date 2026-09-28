package com.clinica.backend.service;

import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.UserRepository;
import com.clinica.backend.security.Roles;
import org.springframework.security.access.AccessDeniedException;

final class ProfessionalAssignmentValidator {

    private ProfessionalAssignmentValidator() {
    }

    static User resolve(Long professionalId, User currentUser, UserRepository userRepository) {
        User professional = professionalId == null || professionalId.equals(currentUser.getId())
                ? currentUser
                : userRepository.findById(professionalId)
                        .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con ID: " + professionalId));

        if (!currentUser.getSpecialty().equals(professional.getSpecialty())) {
            throw new AccessDeniedException("Profesional fuera de la especialidad del usuario");
        }
        if (!professional.isEnabled() || !professional.hasRole(Roles.PROFESIONAL)) {
            throw new IllegalArgumentException("El destinatario debe ser un profesional activo");
        }
        return professional;
    }
}
