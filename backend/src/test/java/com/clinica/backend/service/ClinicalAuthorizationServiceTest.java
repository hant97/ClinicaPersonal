package com.clinica.backend.service;

import com.clinica.backend.model.User;
import com.clinica.backend.security.Roles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClinicalAuthorizationServiceTest {

    private final ClinicalAuthorizationService authorizationService = new ClinicalAuthorizationService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creatorCanManageTheirConfidentialSession() {
        authenticate(user(10L, "PSICOLOGIA", Roles.PROFESIONAL));

        assertDoesNotThrow(() -> authorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L));
    }

    @Test
    void professionalWhoIsAlsoAdministratorCanManageConfidentialSession() {
        authenticate(user(20L, "PSICOLOGIA", Roles.PROFESIONAL, Roles.ADMIN));

        assertDoesNotThrow(() -> authorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L));
    }

    @Test
    void administratorWhoIsNotProfessionalCannotAccessClinicalRecords() {
        authenticate(user(21L, "PSICOLOGIA", Roles.ADMIN));

        assertThrows(AccessDeniedException.class,
                () -> authorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L));
        assertThrows(AccessDeniedException.class, authorizationService::currentProfessional);
    }

    @Test
    void assistantCannotAccessClinicalRecordsEvenInTheirSpecialty() {
        authenticate(user(22L, "PSICOLOGIA", Roles.ASISTENTE));

        AccessDeniedException error = assertThrows(AccessDeniedException.class,
                () -> authorizationService.ensureSameSpecialty("PSICOLOGIA"));
        assertEquals("Solo los profesionales de salud pueden acceder a la información clínica", error.getMessage());
    }

    @Test
    void regularProfessionalCannotManageAnotherProfessionalsConfidentialSession() {
        authenticate(user(30L, "PSICOLOGIA", Roles.PROFESIONAL));

        assertThrows(AccessDeniedException.class,
                () -> authorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L));
    }

    @Test
    void dermatologyProfessionalCannotAccessPsychologyRecord() {
        authenticate(user(40L, "DERMATOLOGIA", Roles.PROFESIONAL, Roles.ADMIN));

        assertThrows(AccessDeniedException.class,
                () -> authorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L));
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private User user(Long id, String specialty, String... roles) {
        User user = new User();
        user.setId(id);
        user.setSpecialty(specialty);
        user.setRoles(Set.of(roles));
        return user;
    }
}
