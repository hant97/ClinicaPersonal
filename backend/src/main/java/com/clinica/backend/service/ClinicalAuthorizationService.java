package com.clinica.backend.service;

import com.clinica.backend.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Applies the clinical-record access policy. A clinical administrator is an
 * ROLE_ADMIN user whose specialty matches the resource specialty.
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

    public void ensureSameSpecialty(String specialty) {
        if (!currentUser().getSpecialty().equals(specialty)) {
            throw new AccessDeniedException("Acceso fuera del ámbito autorizado");
        }
    }

    public boolean isSpecialtyAdministrator(User user, String specialty) {
        return user.getSpecialty().equals(specialty)
                && user.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public void ensureOwnerOrSpecialtyAdministrator(String specialty, Long professionalId) {
        User user = currentUser();
        ensureSameSpecialty(specialty);
        if (!isSpecialtyAdministrator(user, specialty) && !user.getId().equals(professionalId)) {
            throw new AccessDeniedException("Acceso fuera del ámbito autorizado");
        }
    }
}
