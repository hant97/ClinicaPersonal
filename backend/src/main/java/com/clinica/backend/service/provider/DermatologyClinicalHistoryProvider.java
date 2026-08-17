package com.clinica.backend.service.provider;

import com.clinica.backend.dto.ClinicalHistoryDto;
import com.clinica.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DermatologyClinicalHistoryProvider implements ClinicalHistorySectionProvider {

    public static final String SPECIALTY_CODE = "DERMATOLOGIA";

    private final DermatologicalHistoryService dermatologicalHistoryService;
    private final LesionService lesionService;
    private final AuxiliaryExamService auxiliaryExamService;
    private final TreatmentService treatmentService;
    private final ProcedureService procedureService;
    private final EvolutionService evolutionService;

    @Override
    public String getSupportedSpecialty() {
        return SPECIALTY_CODE;
    }

    @Override
    public void populateSections(Long patientId, ClinicalHistoryDto dto, Pageable pageable) {
        dto.setDermatologicalHistory(dermatologicalHistoryService.getHistory(patientId));
        dto.setLesions(lesionService.getLesions(patientId, pageable).getContent());
        dto.setAuxiliaryExams(auxiliaryExamService.getExams(patientId, pageable).getContent());
        dto.setTreatments(treatmentService.getTreatments(patientId, pageable).getContent());
        dto.setProcedures(procedureService.getProcedures(patientId, pageable).getContent());
        dto.setEvolutions(evolutionService.getEvolutions(patientId, pageable).getContent());
    }
}
