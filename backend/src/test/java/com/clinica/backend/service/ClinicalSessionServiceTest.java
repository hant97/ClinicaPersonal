package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalSessionDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.ClinicalSession;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalSessionServiceTest {

    @Mock
    private ClinicalSessionRepository sessionRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;
    @InjectMocks
    private ClinicalSessionService clinicalSessionService;

    private ClinicalSession confidentialSession;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = user(10L, "PSICOLOGIA", "ROLE_STAFF");
        Patient patient = new Patient();
        patient.setId(7L);
        confidentialSession = new ClinicalSession();
        confidentialSession.setId(4L);
        confidentialSession.setPatient(patient);
        confidentialSession.setSpecialty("PSICOLOGIA");
        confidentialSession.setProfessionalId(10L);
        confidentialSession.setConfidential(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void confidentialSessionCanBeReadByItsCreator() {
        when(sessionRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.of(confidentialSession));

        ClinicalSessionDto result = clinicalSessionService.getSessionById(4L);

        assertEquals(4L, result.getId());
        verify(clinicalAuthorizationService).ensureSameSpecialty("PSICOLOGIA");
        verify(clinicalAuthorizationService).ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L);
    }

    @Test
    void confidentialSessionCanBeEditedBySpecialtyAdministrator() {
        ClinicalSessionDto request = new ClinicalSessionDto();
        request.setStatus("COMPLETADA");
        when(sessionRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.of(confidentialSession));
        when(sessionRepository.save(confidentialSession)).thenReturn(confidentialSession);

        ClinicalSessionDto result = clinicalSessionService.updateSession(4L, request);

        assertEquals("COMPLETADA", result.getStatus());
        verify(clinicalAuthorizationService).ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L);
    }

    @Test
    void confidentialSessionRejectsUserWithoutOwnership() {
        when(sessionRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.of(confidentialSession));
        org.mockito.Mockito.doThrow(new AccessDeniedException("Acceso fuera del ámbito autorizado"))
                .when(clinicalAuthorizationService).ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", 10L);

        assertThrows(AccessDeniedException.class, () -> clinicalSessionService.getSessionById(4L));
    }

    @Test
    void crossSpecialtyAccessIsForbidden() {
        when(sessionRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.of(confidentialSession));
        org.mockito.Mockito.doThrow(new AccessDeniedException("Acceso fuera del ámbito autorizado"))
                .when(clinicalAuthorizationService).ensureSameSpecialty("PSICOLOGIA");

        assertThrows(AccessDeniedException.class, () -> clinicalSessionService.getSessionById(4L));
    }

    @Test
    void createSessionUsesAuthenticatedProfessionalInsteadOfClientValue() {
        Patient patient = new Patient();
        patient.setId(7L);
        ClinicalSessionDto request = new ClinicalSessionDto();
        request.setPatientId(7L);
        request.setProfessionalId(999L);
        when(clinicalAuthorizationService.currentUser()).thenReturn(owner);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(sessionRepository.save(any(ClinicalSession.class))).thenAnswer(invocation -> {
            ClinicalSession session = invocation.getArgument(0);
            session.setId(11L);
            return session;
        });

        ClinicalSessionDto result = clinicalSessionService.createSession(request);

        assertEquals(10L, result.getProfessionalId());
        ArgumentCaptor<ClinicalSession> sessionCaptor = ArgumentCaptor.forClass(ClinicalSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertEquals(10L, sessionCaptor.getValue().getProfessionalId());
        assertNotNull(result.getId());
    }

    @Test
    void deletedSessionIsNotFound() {
        when(sessionRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clinicalSessionService.getSessionById(4L));
    }

    private User user(Long id, String specialty, String role) {
        User user = new User();
        user.setId(id);
        user.setSpecialty(specialty);
        user.setRoles(Set.of(role));
        return user;
    }
}
