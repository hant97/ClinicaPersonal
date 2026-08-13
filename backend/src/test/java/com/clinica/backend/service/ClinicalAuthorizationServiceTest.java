package com.clinica.backend.service;

import com.clinica.backend.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClinicalAuthorizationServiceTest {

    private final ClinicalAuthorizationService authorizationService = new ClinicalAuthorizationService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creatorCanManageTheirConfidentialSession() {
        authenticate(user(10L, "PSICOLOGIA", "ROLE_STAFF"));

        assertDoesNotThrow(() -> authorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L));
    }

    @Test
    void administratorOfSameSpecialtyCanManageConfidentialSession() {
        authenticate(user(20L, "PSICOLOGIA", "ROLE_ADMIN"));

        assertDoesNotThrow(() -> authorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L));
    }

    @Test
    void regularProfessionalCannotManageAnotherProfessionalsConfidentialSession() {
        authenticate(user(30L, "PSICOLOGIA", "ROLE_STAFF"));

        assertThrows(AccessDeniedException.class,
                () -> authorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L));
    }

    @Test
    void dermatologyProfessionalCannotAccessPsychologyRecord() {
        authenticate(user(40L, "DERMATOLOGIA", "ROLE_ADMIN"));

        assertThrows(AccessDeniedException.class,
                () -> authorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L));
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private User user(Long id, String specialty, String role) {
        User user = new User();
        user.setId(id);
        user.setSpecialty(specialty);
        user.setRoles(Set.of(role));
        return user;
    }
}
