package com.clinica.backend.service;

import com.clinica.backend.model.User;
import com.clinica.backend.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Applies the clinical-record access policy (docs/POLITICA-AUTORIZACION-CLINICA.md).
 * Clinical information is only available to users with {@link Roles#PROFESIONAL}. A clinical
 * administrator is a professional who also has {@link Roles#ADMIN} and whose specialty
 * matches the resource specialty.
 */
@Service
@RequiredArgsConstructor
public class ClinicalAuthorizationService {

    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AccessDeniedException("Acceso no autorizado");
        }
        return user;
    }

    /** Authenticated user, required to be a health professional. */
    public User currentProfessional() {
        User user = currentUser();
        if (!user.hasRole(Roles.PROFESIONAL)) {
            throw new AccessDeniedException("Solo los profesionales de salud pueden acceder a la información clínica");
        }
        return user;
    }

    public void ensureSameSpecialty(String specialty) {
        if (!currentProfessional().getSpecialty().equals(specialty)) {
            throw new AccessDeniedException("Acceso fuera del ámbito autorizado");
        }
    }

    public boolean isSpecialtyAdministrator(User user, String specialty) {
        return user.getSpecialty().equals(specialty)
                && user.hasRole(Roles.PROFESIONAL)
                && user.hasRole(Roles.ADMIN);
    }

    public void ensureOwnerOrSpecialtyAdministrator(String specialty, Long professionalId) {
        User user = currentProfessional();
        ensureSameSpecialty(specialty);
        if (!isSpecialtyAdministrator(user, specialty) && !user.getId().equals(professionalId)) {
            throw new AccessDeniedException("Acceso fuera del ámbito autorizado");
        }
    }
}
