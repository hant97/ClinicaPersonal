package com.clinica.backend.service;

import com.clinica.backend.dto.GeneralHistoryDto;
import com.clinica.backend.model.GeneralHistory;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.GeneralHistoryRepository;
import com.clinica.backend.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralHistoryServiceTest {

    @Mock
    private GeneralHistoryRepository repository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;
    @InjectMocks
    private GeneralHistoryService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = user(10L, "PSICOLOGIA", "ROLE_STAFF");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getGeneralHistoryWhenMissingReturnsEmptyDtoWithPatientIdAndSpecialty() {
        Patient patient = new Patient();
        patient.setId(7L);
        when(clinicalAuthorizationService.currentUser()).thenReturn(user);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(repository.findByPatientIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.empty());

        GeneralHistoryDto result = service.getGeneralHistory(7L);

        assertNull(result.getId());
        assertEquals(7L, result.getPatientId());
        assertEquals("PSICOLOGIA", result.getSpecialty());
    }

    @Test
    void upsertGeneralHistoryCreatesWhenMissingUsingAuthenticatedProfessional() {
        Patient patient = new Patient();
        patient.setId(7L);
        when(clinicalAuthorizationService.currentUser()).thenReturn(user);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(repository.findByPatientIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.empty());
        when(repository.save(any(GeneralHistory.class))).thenAnswer(invocation -> {
            GeneralHistory history = invocation.getArgument(0);
            history.setId(1L);
            return history;
        });

        GeneralHistoryDto dto = new GeneralHistoryDto();
        dto.setPathologicalHistory("Asma");
        GeneralHistoryDto result = service.upsertGeneralHistory(7L, dto);

        assertEquals(1L, result.getId());
        assertEquals(10L, result.getProfessionalId());
        assertEquals("PSICOLOGIA", result.getSpecialty());
        assertEquals("Asma", result.getPathologicalHistory());
    }

    private User user(Long id, String specialty, String role) {
        User u = new User();
        u.setId(id);
        u.setSpecialty(specialty);
        u.setRoles(Set.of(role));
        return u;
    }
}
