package com.clinica.backend.service;

import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AllergyRepository;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LogicalDeleteIntegrityTest {

    private PatientRepository patientRepository;
    private AllergyRepository allergyRepository;
    private ClinicalSessionRepository sessionRepository;
    private ClinicalAuthorizationService authService;
    private AllergyService allergyService;
    private ClinicalSessionService sessionService;

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        allergyRepository = mock(AllergyRepository.class);
        sessionRepository = mock(ClinicalSessionRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        authService = mock(ClinicalAuthorizationService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);

        allergyService = new AllergyService(allergyRepository, patientRepository, authService);
        sessionService = new ClinicalSessionService(sessionRepository, patientRepository, appointmentRepository, authService, auditLogService);

        User user = new User();
        user.setId(1L);
        user.setUsername("doctor");
        user.setSpecialty("PSICOLOGIA");
        user.setRoles(Set.of("ROLE_ADMIN"));

        when(authService.currentUser()).thenReturn(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllergiesShouldThrowNotFoundWhenPatientIsDeleted() {
        Long deletedPatientId = 42L;
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(deletedPatientId, "PSICOLOGIA"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                allergyService.getAllergies(deletedPatientId, PageRequest.of(0, 10))
        );

        verify(allergyRepository, never()).findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void getClinicalSessionsShouldThrowNotFoundWhenPatientIsDeleted() {
        Long deletedPatientId = 42L;
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(deletedPatientId, "PSICOLOGIA"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.getSessionsByPatientId(deletedPatientId, PageRequest.of(0, 10))
        );

        verify(sessionRepository, never()).findByPatientIdAndSpecialtyAndDeletedFalseOrderBySessionDateDescStartTimeDesc(any(), any(), any());
    }
}
