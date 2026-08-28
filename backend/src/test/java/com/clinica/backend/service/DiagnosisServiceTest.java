package com.clinica.backend.service;

import com.clinica.backend.dto.DiagnosisDto;
import com.clinica.backend.model.Diagnosis;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.DiagnosisRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.mapper.DiagnosisMapperImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisServiceTest {

    @Mock
    private DiagnosisRepository repository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;
    @Spy
    private DiagnosisMapperImpl diagnosisMapper = new DiagnosisMapperImpl();
    @InjectMocks
    private DiagnosisService service;

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
    void createDiagnosisUsesAuthenticatedProfessionalAndSpecialty() {
        Patient patient = new Patient();
        patient.setId(7L);
        when(clinicalAuthorizationService.currentUser()).thenReturn(user);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(repository.save(any(Diagnosis.class))).thenAnswer(invocation -> {
            Diagnosis diagnosis = invocation.getArgument(0);
            diagnosis.setId(3L);
            return diagnosis;
        });

        DiagnosisDto dto = new DiagnosisDto();
        dto.setDescription("Trastorno de ansiedad");
        DiagnosisDto result = service.createDiagnosis(7L, dto);

        assertEquals(3L, result.getId());
        assertEquals(10L, result.getProfessionalId());
        assertEquals("PSICOLOGIA", result.getSpecialty());
        assertEquals("ACTIVO", result.getStatus());
        assertEquals("Trastorno de ansiedad", result.getDescription());
    }

    private User user(Long id, String specialty, String role) {
        User u = new User();
        u.setId(id);
        u.setSpecialty(specialty);
        u.setRoles(Set.of(role));
        return u;
    }
}
