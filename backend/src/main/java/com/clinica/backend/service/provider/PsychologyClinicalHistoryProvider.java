package com.clinica.backend.service.provider;

import com.clinica.backend.dto.ClinicalHistoryDto;
import com.clinica.backend.service.PsychologyEvaluationService;
import com.clinica.backend.service.TherapeuticPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PsychologyClinicalHistoryProvider implements ClinicalHistorySectionProvider {

    public static final String SPECIALTY_CODE = "PSICOLOGIA";

    private final PsychologyEvaluationService psychologyEvaluationService;
    private final TherapeuticPlanService therapeuticPlanService;

    @Override
    public String getSupportedSpecialty() {
        return SPECIALTY_CODE;
    }

    @Override
    public void populateSections(Long patientId, ClinicalHistoryDto dto, Pageable pageable) {
        Pageable byEvaluationDate = PageRequest.of(0, pageable.getPageSize(), Sort.by("evaluationDate").descending());
        dto.setPsychologyEvaluations(psychologyEvaluationService.getEvaluationsByPatientId(patientId, byEvaluationDate).getContent());
        dto.setTherapeuticPlans(therapeuticPlanService.getPlans(patientId, pageable).getContent());
    }
}
