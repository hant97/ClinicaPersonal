package com.clinica.backend.service;

import com.clinica.backend.dto.AssessmentDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Assessment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.PsychometricTest;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AssessmentRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PsychometricTestRepository;
import com.clinica.backend.mapper.AssessmentMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private PsychometricTestRepository psychometricTestRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;

    @Spy
    private AssessmentMapperImpl assessmentMapper = new AssessmentMapperImpl();
    @InjectMocks
    private AssessmentService assessmentService;

    private User user;
    private Patient patient;
    private PsychometricTest test;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setSpecialty("PSICOLOGIA");
        user.setRoles(Set.of("ROLE_STAFF"));

        patient = new Patient();
        patient.setId(10L);
        patient.setSpecialty("PSICOLOGIA");

        test = new PsychometricTest();
        test.setId(5L);
        test.setName("PHQ-9");
        test.setQuestionsJson("[]");
        test.setInterpretationJson("[{\"min\":0,\"max\":4,\"label\":\"Mínima\",\"color\":\"green\"}]");
    }

    @Test
    @DisplayName("getPatientEvolution should return full list of assessments ordered chronologically")
    void getPatientEvolution_ShouldReturnChronologicalAssessments() {
        when(clinicalAuthorizationService.currentProfessional()).thenReturn(user);
        when(patientRepository.existsByIdAndSpecialtyAndDeletedFalse(10L, "PSICOLOGIA")).thenReturn(true);

        Assessment a1 = new Assessment();
        a1.setId(101L);
        a1.setPatient(patient);
        a1.setPsychometricTest(test);
        a1.setAssessmentDate(LocalDateTime.of(2026, 1, 15, 10, 0));
        a1.setTotalScore(18);

        Assessment a2 = new Assessment();
        a2.setId(102L);
        a2.setPatient(patient);
        a2.setPsychometricTest(test);
        a2.setAssessmentDate(LocalDateTime.of(2026, 2, 15, 10, 0));
        a2.setTotalScore(12);

        when(assessmentRepository.findByPatientIdOrderByAssessmentDateAsc(10L)).thenReturn(List.of(a1, a2));

        List<AssessmentDto> evolution = assessmentService.getPatientEvolution(10L);

        assertNotNull(evolution);
        assertEquals(2, evolution.size());
        assertEquals(101L, evolution.get(0).getId());
        assertEquals(18, evolution.get(0).getTotalScore());
        assertEquals("PHQ-9", evolution.get(0).getTestName());
        assertEquals(102L, evolution.get(1).getId());
        assertEquals(12, evolution.get(1).getTotalScore());
    }

    @Test
    @DisplayName("getPatientEvolution should throw ResourceNotFoundException if patient does not exist in specialty")
    void getPatientEvolution_PatientNotFound_ShouldThrow() {
        when(clinicalAuthorizationService.currentProfessional()).thenReturn(user);
        when(patientRepository.existsByIdAndSpecialtyAndDeletedFalse(99L, "PSICOLOGIA")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> assessmentService.getPatientEvolution(99L));
    }

    @Test
    @DisplayName("saveAssessment should correctly persist assessment and return mapped DTO")
    void saveAssessment_ShouldPersistAndReturnDto() {
        when(clinicalAuthorizationService.currentProfessional()).thenReturn(user);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(10L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(psychometricTestRepository.findById(5L)).thenReturn(Optional.of(test));

        Assessment saved = new Assessment();
        saved.setId(200L);
        saved.setPatient(patient);
        saved.setPsychometricTest(test);
        saved.setTotalScore(15);
        saved.setAnswersJson("{\"1\":3}");
        saved.setNotes("Seguimiento");

        when(assessmentRepository.save(any(Assessment.class))).thenReturn(saved);

        AssessmentDto input = new AssessmentDto();
        input.setPatientId(10L);
        input.setPsychometricTestId(5L);
        input.setTotalScore(15);
        input.setAnswersJson("{\"1\":3}");
        input.setNotes("Seguimiento");

        AssessmentDto result = assessmentService.saveAssessment(input);

        assertNotNull(result);
        assertEquals(200L, result.getId());
        assertEquals(10L, result.getPatientId());
        assertEquals(5L, result.getPsychometricTestId());
        assertEquals("PHQ-9", result.getTestName());
        assertEquals(15, result.getTotalScore());
    }
}
