package com.clinica.backend.service;

import com.clinica.backend.dto.PsychologyEvaluationDto;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.PsychologyEvaluation;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PsychologyEvaluationRepository;
import com.clinica.backend.mapper.PsychologyEvaluationMapperImpl;
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
class PsychologyEvaluationServiceTest {

    @Mock
    private PsychologyEvaluationRepository repository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;
    @Spy
    private PsychologyEvaluationMapperImpl psychologyEvaluationMapper = new PsychologyEvaluationMapperImpl();
    @InjectMocks
    private PsychologyEvaluationService service;

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
    void createEvaluationUsesAuthenticatedProfessionalInsteadOfClientValue() {
        Patient patient = new Patient();
        patient.setId(7L);
        when(clinicalAuthorizationService.currentUser()).thenReturn(user);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(repository.save(any(PsychologyEvaluation.class))).thenAnswer(invocation -> {
            PsychologyEvaluation evaluation = invocation.getArgument(0);
            evaluation.setId(5L);
            return evaluation;
        });

        PsychologyEvaluationDto dto = new PsychologyEvaluationDto();
        dto.setPatientId(7L);
        dto.setProfessionalId(999L);
        dto.setInitialEvaluation("Motivo de consulta");

        PsychologyEvaluationDto result = service.createEvaluation(dto);

        assertEquals(5L, result.getId());
        assertEquals(10L, result.getProfessionalId());
        assertEquals("Motivo de consulta", result.getInitialEvaluation());
    }

    private User user(Long id, String specialty, String role) {
        User u = new User();
        u.setId(id);
        u.setSpecialty(specialty);
        u.setRoles(Set.of(role));
        return u;
    }
}
