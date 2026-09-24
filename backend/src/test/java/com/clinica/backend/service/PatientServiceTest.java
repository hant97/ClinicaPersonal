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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private ClinicalFileStorage clinicalFileStorage;
    private AuditLogService auditLogService;
    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        riskAlertRepository = mock(RiskAlertRepository.class);
        clinicalFileStorage = mock(ClinicalFileStorage.class);
        auditLogService = mock(AuditLogService.class);
        patientService = new PatientService(patientRepository, riskAlertRepository, clinicalFileStorage, auditLogService, new PatientMapperImpl());

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

    @Test
    void uploadPhotoStoresPrivatelyAndReturnsAuthenticatedUrl() {
        Patient patient = patientWithPhoto(7L, "patients/old-photo.png");
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(clinicalFileStorage.store(any(), eq("patients"))).thenReturn("patients/0f1e2d3c-new.png");

        PatientDto dto = patientService.uploadPhoto(7L, TestUploads.png("foto.png"));

        assertEquals("patients/0f1e2d3c-new.png", patient.getPhotoKey());
        assertEquals("/api/v1/patients/7/photo?v=0f1e2d3c-new", dto.getPhotoUrl());
        // Sin transacción activa la foto anterior se elimina de inmediato.
        verify(clinicalFileStorage).delete("patients/old-photo.png");
    }

    @Test
    void uploadPhotoDeletesTheNewFileAndKeepsThePreviousOneOnRollback() {
        Patient patient = patientWithPhoto(7L, "patients/old-photo.png");
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(clinicalFileStorage.store(any(), eq("patients"))).thenReturn("patients/new-photo.png");

        TransactionSynchronizationManager.initSynchronization();
        try {
            patientService.uploadPhoto(7L, TestUploads.png("foto.png"));
            verify(clinicalFileStorage, never()).delete(any());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(clinicalFileStorage).delete("patients/new-photo.png");
        verify(clinicalFileStorage, never()).delete("patients/old-photo.png");
    }

    @Test
    void loadPhotoIsScopedToTheAuthenticatedSpecialty() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.loadPhoto(7L));
        verify(clinicalFileStorage, never()).load(any());
    }

    @Test
    void loadPhotoFailsWhenPatientHasNoPhoto() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patientWithPhoto(7L, null)));

        assertThrows(ResourceNotFoundException.class, () -> patientService.loadPhoto(7L));
    }

    @Test
    void patientWithoutPhotoHasNoPhotoUrl() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patientWithPhoto(7L, null)));

        assertNull(patientService.getPatientById(7L).getPhotoUrl());
    }

    private Patient patientWithPhoto(Long id, String photoKey) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setFirstName("Ana");
        patient.setLastName("Pérez");
        patient.setSpecialty("PSICOLOGIA");
        patient.setPhotoKey(photoKey);
        return patient;
    }
}
