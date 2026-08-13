package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalHistoryDto {
    private Long patientId;
    private String specialty;
    private GeneralHistoryDto generalHistory;
    private List<AllergyDto> allergies;
    private List<MedicationDto> medications;
    private List<DiagnosisDto> diagnoses;
    private List<PsychologyEvaluationDto> psychologyEvaluations;
    private List<TherapeuticPlanDto> therapeuticPlans;
    private DermatologicalHistoryDto dermatologicalHistory;
    private List<LesionDto> lesions;
    private List<AuxiliaryExamDto> auxiliaryExams;
    private List<TreatmentDto> treatments;
    private List<ProcedureDto> procedures;
    private List<EvolutionDto> evolutions;
}
