package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalHistoryDto;
import com.clinica.backend.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicalHistoryService {

    private static final int AGGREGATE_PAGE_SIZE = 1000;

    private final GeneralHistoryService generalHistoryService;
    private final AllergyService allergyService;
    private final MedicationService medicationService;
    private final DiagnosisService diagnosisService;
    private final PsychologyEvaluationService psychologyEvaluationService;
    private final TherapeuticPlanService therapeuticPlanService;
    private final DermatologicalHistoryService dermatologicalHistoryService;
    private final LesionService lesionService;
    private final AuxiliaryExamService auxiliaryExamService;
    private final TreatmentService treatmentService;
    private final ProcedureService procedureService;
    private final EvolutionService evolutionService;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public ClinicalHistoryDto getClinicalHistory(Long patientId) {
        User user = clinicalAuthorizationService.currentUser();
        Pageable pageable = PageRequest.of(0, AGGREGATE_PAGE_SIZE);

        ClinicalHistoryDto dto = new ClinicalHistoryDto();
        dto.setPatientId(patientId);
        dto.setSpecialty(user.getSpecialty());

        dto.setGeneralHistory(generalHistoryService.getGeneralHistory(patientId));
        dto.setAllergies(allergyService.getAllergies(patientId, pageable).getContent());
        dto.setMedications(medicationService.getMedications(patientId, pageable).getContent());
        dto.setDiagnoses(diagnosisService.getDiagnoses(patientId, pageable).getContent());

        if ("PSICOLOGIA".equals(user.getSpecialty())) {
            Pageable byEvaluationDate = PageRequest.of(0, AGGREGATE_PAGE_SIZE, Sort.by("evaluationDate").descending());
            dto.setPsychologyEvaluations(psychologyEvaluationService.getEvaluationsByPatientId(patientId, byEvaluationDate).getContent());
            dto.setTherapeuticPlans(therapeuticPlanService.getPlans(patientId, pageable).getContent());
        } else if ("DERMATOLOGIA".equals(user.getSpecialty())) {
            dto.setDermatologicalHistory(dermatologicalHistoryService.getHistory(patientId));
            dto.setLesions(lesionService.getLesions(patientId, pageable).getContent());
            dto.setAuxiliaryExams(auxiliaryExamService.getExams(patientId, pageable).getContent());
            dto.setTreatments(treatmentService.getTreatments(patientId, pageable).getContent());
            dto.setProcedures(procedureService.getProcedures(patientId, pageable).getContent());
            dto.setEvolutions(evolutionService.getEvolutions(patientId, pageable).getContent());
        }

        return dto;
    }
}
