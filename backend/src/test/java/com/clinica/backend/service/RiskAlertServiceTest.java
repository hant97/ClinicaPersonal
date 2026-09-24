package com.clinica.backend.service;

import com.clinica.backend.dto.RiskAlertDto;
import com.clinica.backend.mapper.RiskAlertMapperImpl;
import com.clinica.backend.model.RiskAlert;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.RiskAlertRepository;
import com.clinica.backend.security.Roles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskAlertServiceTest {

    private RiskAlertRepository alertRepository;
    private PatientRepository patientRepository;
    private RiskAlertService service;

    @BeforeEach
    void setUp() {
        alertRepository = mock(RiskAlertRepository.class);
        patientRepository = mock(PatientRepository.class);
        service = new RiskAlertService(alertRepository, patientRepository, new RiskAlertMapperImpl(),
                new ClinicalAuthorizationService());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAlertInTheProfessionalsSpecialtyForAnExistingPatient() {
        authenticate(Roles.PROFESIONAL);
        when(patientRepository.existsByIdAndSpecialtyAndDeletedFalse(5L, "PSICOLOGIA")).thenReturn(true);
        when(alertRepository.save(any(RiskAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RiskAlertDto dto = new RiskAlertDto();
        dto.setPatientId(5L);
        dto.setType("SUICIDA");
        dto.setLevel("ALTO");

        RiskAlertDto created = service.createAlert(dto);

        assertNotNull(created);
        assertEquals("PSICOLOGIA", created.getSpecialty());
    }

    @Test
    void rejectsAlertsForPatientsOutsideTheSpecialty() {
        authenticate(Roles.PROFESIONAL);
        when(patientRepository.existsByIdAndSpecialtyAndDeletedFalse(6L, "PSICOLOGIA")).thenReturn(false);
        RiskAlertDto dto = new RiskAlertDto();
        dto.setPatientId(6L);

        assertThrows(IllegalArgumentException.class, () -> service.createAlert(dto));
        verify(alertRepository, never()).save(any());
    }

    @Test
    void resolvingAnotherSpecialtysAlertIsForbidden() {
        authenticate(Roles.PROFESIONAL);
        RiskAlert alert = new RiskAlert();
        alert.setId(1L);
        alert.setSpecialty("DERMATOLOGIA");
        alert.setActive(true);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(AccessDeniedException.class, () -> service.resolveAlert(1L));
    }

    @Test
    void resolvingMarksTheAlertInactiveWithTimestamp() {
        authenticate(Roles.PROFESIONAL);
        RiskAlert alert = new RiskAlert();
        alert.setId(2L);
        alert.setSpecialty("PSICOLOGIA");
        alert.setActive(true);
        when(alertRepository.findById(2L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(RiskAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.resolveAlert(2L);

        assertFalse(alert.isActive());
        assertNotNull(alert.getResolvedAt());
    }

    @Test
    void assistantsCannotReadAlertDetails() {
        authenticate(Roles.ASISTENTE);

        assertThrows(AccessDeniedException.class, () -> service.getAllActiveAlerts(Pageable.ofSize(10)));
        verify(alertRepository, never()).findBySpecialtyAndActiveTrueOrderByCreatedAtDesc(any(), any());
    }

    private void authenticate(String role) {
        User user = new User();
        user.setId(1L);
        user.setSpecialty("PSICOLOGIA");
        user.setRoles(Set.of(role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
