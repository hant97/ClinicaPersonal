package com.clinica.backend.service;

import com.clinica.backend.dto.PatientStatsDto;
import com.clinica.backend.mapper.PatientMapperImpl;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.RiskAlertRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.clinica.backend.dto.PatientDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Patient;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatientServiceTest {

    private PatientRepository patientRepository;
    private RiskAlertRepository riskAlertRepository;
    private WebsiteFileStorage websiteFileStorage;
    private AuditLogService auditLogService;
    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        riskAlertRepository = mock(RiskAlertRepository.class);
        websiteFileStorage = mock(WebsiteFileStorage.class);
        auditLogService = mock(AuditLogService.class);
        patientService = new PatientService(patientRepository, riskAlertRepository, websiteFileStorage, auditLogService, new PatientMapperImpl());

        User user = new User();
        user.setId(1L);
        user.setUsername("doctor");
        user.setSpecialty("PSICOLOGIA");
        user.setRoles(Set.of("ROLE_ADMIN"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getStatsShouldAggregateCounters() {
        when(patientRepository.countBySpecialtyAndDeletedFalse("PSICOLOGIA")).thenReturn(120L);
        when(patientRepository.countNewPatientsBetween(eq("PSICOLOGIA"), any(), any())).thenReturn(8L);
        when(riskAlertRepository.countDistinctPatientsWithActiveAlerts("PSICOLOGIA")).thenReturn(5L);
        when(patientRepository.countMinorsBySpecialty(eq("PSICOLOGIA"), any())).thenReturn(12L);

        PatientStatsDto stats = patientService.getStats();

        assertEquals(120L, stats.getTotalPatients());
        assertEquals(8L, stats.getNewThisMonth());
        assertEquals(5L, stats.getWithActiveAlerts());
        assertEquals(12L, stats.getMinors());
    }

    @Test
    void getPatientByIdOrUuidShouldResolveByUuid() {
        UUID uuid = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setUuid(uuid);
        patient.setFirstName("Carlos");
        patient.setLastName("López");
        patient.setSpecialty("PSICOLOGIA");

        when(patientRepository.findByUuidAndSpecialtyAndDeletedFalse(uuid, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));
        when(riskAlertRepository.existsByPatientIdAndSpecialtyAndActiveTrue(10L, "PSICOLOGIA"))
                .thenReturn(false);

        PatientDto result = patientService.getPatientByIdOrUuid(uuid.toString());

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(uuid, result.getUuid());
        assertEquals("Carlos", result.getFirstName());
    }

    @Test
    void getPatientByIdOrUuidShouldResolveByNumericId() {
        Patient patient = new Patient();
        patient.setId(15L);
        patient.setUuid(UUID.randomUUID());
        patient.setFirstName("María");
        patient.setLastName("González");
        patient.setSpecialty("PSICOLOGIA");

        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(15L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));
        when(riskAlertRepository.existsByPatientIdAndSpecialtyAndActiveTrue(15L, "PSICOLOGIA"))
                .thenReturn(false);

        PatientDto result = patientService.getPatientByIdOrUuid("15");

        assertNotNull(result);
        assertEquals(15L, result.getId());
        assertEquals("María", result.getFirstName());
    }

    @Test
    void getPatientByIdOrUuidShouldThrowWhenNotFound() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(999L, "PSICOLOGIA"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.getPatientByIdOrUuid("999"));
    }
}
