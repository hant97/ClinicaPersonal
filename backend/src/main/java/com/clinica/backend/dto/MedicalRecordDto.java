package com.clinica.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MedicalRecordDto {
    private Long id;
    private Long patientId;
    private LocalDateTime sessionDate;
    private String reasonForConsultation;
    private String evolutionNotes;
    private String presumptiveDiagnosis;
    private String agreementsAndTasks;
    private String dynamicData;
}
