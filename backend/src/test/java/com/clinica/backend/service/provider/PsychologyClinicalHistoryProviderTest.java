package com.clinica.backend.service.provider;

import com.clinica.backend.dto.ClinicalHistoryDto;
import com.clinica.backend.dto.PsychologyEvaluationDto;
import com.clinica.backend.dto.TherapeuticPlanDto;
import com.clinica.backend.service.PsychologyEvaluationService;
import com.clinica.backend.service.TherapeuticPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PsychologyClinicalHistoryProviderTest {

    @Mock
    private PsychologyEvaluationService psychologyEvaluationService;

    @Mock
    private TherapeuticPlanService therapeuticPlanService;

    @InjectMocks
    private PsychologyClinicalHistoryProvider provider;

    @Test
    @DisplayName("getSupportedSpecialty should return PSICOLOGIA")
    void getSupportedSpecialty_ShouldReturnPsychology() {
        assertEquals("PSICOLOGIA", provider.getSupportedSpecialty());
    }

    @Test
    @DisplayName("populateSections should load psychology evaluations and therapeutic plans")
    void populateSections_ShouldPopulateDto() {
        ClinicalHistoryDto dto = new ClinicalHistoryDto();
        dto.setPatientId(5L);
        dto.setSpecialty("PSICOLOGIA");
        Pageable pageable = PageRequest.of(0, 50);

        PsychologyEvaluationDto eval = new PsychologyEvaluationDto();
        eval.setId(10L);
        eval.setPatientId(5L);

        TherapeuticPlanDto plan = new TherapeuticPlanDto();
        plan.setId(20L);
        plan.setPatientId(5L);

        when(psychologyEvaluationService.getEvaluationsByPatientId(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eval)));
        when(therapeuticPlanService.getPlans(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(plan)));

        provider.populateSections(5L, dto, pageable);

        assertNotNull(dto.getPsychologyEvaluations());
        assertEquals(1, dto.getPsychologyEvaluations().size());
        assertEquals(10L, dto.getPsychologyEvaluations().get(0).getId());

        assertNotNull(dto.getTherapeuticPlans());
        assertEquals(1, dto.getTherapeuticPlans().size());
        assertEquals(20L, dto.getTherapeuticPlans().get(0).getId());

        verify(psychologyEvaluationService, times(1)).getEvaluationsByPatientId(eq(5L), any(Pageable.class));
        verify(therapeuticPlanService, times(1)).getPlans(eq(5L), any(Pageable.class));
    }
}
