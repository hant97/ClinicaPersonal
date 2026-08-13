package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalHistoryDto;
import com.clinica.backend.dto.DermatologicalHistoryDto;
import com.clinica.backend.dto.GeneralHistoryDto;
import com.clinica.backend.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalHistoryServiceTest {

    @Mock
    private GeneralHistoryService generalHistoryService;
    @Mock
    private AllergyService allergyService;
    @Mock
    private MedicationService medicationService;
    @Mock
    private DiagnosisService diagnosisService;
    @Mock
    private PsychologyEvaluationService psychologyEvaluationService;
    @Mock
    private TherapeuticPlanService therapeuticPlanService;
    @Mock
    private DermatologicalHistoryService dermatologicalHistoryService;
    @Mock
    private LesionService lesionService;
    @Mock
    private AuxiliaryExamService auxiliaryExamService;
    @Mock
    private TreatmentService treatmentService;
    @Mock
    private ProcedureService procedureService;
    @Mock
    private EvolutionService evolutionService;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;

    @InjectMocks
    private ClinicalHistoryService service;

    private User user;

    @BeforeEach
    void setUp() {
        when(generalHistoryService.getGeneralHistory(7L)).thenReturn(new GeneralHistoryDto());
        when(allergyService.getAllergies(any(), any())).thenReturn(emptyPage());
        when(medicationService.getMedications(any(), any())).thenReturn(emptyPage());
        when(diagnosisService.getDiagnoses(any(), any())).thenReturn(emptyPage());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsPsychologyBranchForPsychologyUser() {
        user = user(10L, "PSICOLOGIA", "ROLE_ADMIN");
        when(clinicalAuthorizationService.currentUser()).thenReturn(user);
        when(psychologyEvaluationService.getEvaluationsByPatientId(any(), any())).thenReturn(emptyPage());
        when(therapeuticPlanService.getPlans(any(), any())).thenReturn(emptyPage());

        ClinicalHistoryDto result = service.getClinicalHistory(7L);

        assertEquals("PSICOLOGIA", result.getSpecialty());
        assertNotNull(result.getGeneralHistory());
        assertNotNull(result.getPsychologyEvaluations());
        assertNotNull(result.getTherapeuticPlans());
        assertNull(result.getDermatologicalHistory());
        assertNull(result.getLesions());
    }

    @Test
    void returnsDermatologyBranchForDermatologyUser() {
        user = user(10L, "DERMATOLOGIA", "ROLE_ADMIN");
        when(clinicalAuthorizationService.currentUser()).thenReturn(user);
        when(dermatologicalHistoryService.getHistory(7L)).thenReturn(new DermatologicalHistoryDto());
        when(lesionService.getLesions(any(), any())).thenReturn(emptyPage());
        when(auxiliaryExamService.getExams(any(), any())).thenReturn(emptyPage());
        when(treatmentService.getTreatments(any(), any())).thenReturn(emptyPage());
        when(procedureService.getProcedures(any(), any())).thenReturn(emptyPage());
        when(evolutionService.getEvolutions(any(), any())).thenReturn(emptyPage());

        ClinicalHistoryDto result = service.getClinicalHistory(7L);

        assertEquals("DERMATOLOGIA", result.getSpecialty());
        assertNotNull(result.getGeneralHistory());
        assertNotNull(result.getDermatologicalHistory());
        assertNotNull(result.getLesions());
        assertNull(result.getPsychologyEvaluations());
        assertNull(result.getTherapeuticPlans());
    }

    private <T> Page<T> emptyPage() {
        return new PageImpl<>(List.of());
    }

    private User user(Long id, String specialty, String role) {
        User u = new User();
        u.setId(id);
        u.setSpecialty(specialty);
        u.setRoles(Set.of(role));
        return u;
    }
}
