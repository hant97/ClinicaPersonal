package com.clinica.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MedicalRecordDto {
    private Long id;
    private Long patientId;
    private String diagnosis;
    private String currentMedication;
    private String treatmentPlan;
    private String treatmentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
