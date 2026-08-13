package com.clinica.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class MedicalRecordDto {
    private Long id;
    @NotNull(message = "El paciente es obligatorio") private Long patientId;
    private String diagnosis;
    private String currentMedication;
    private String treatmentPlan;
    @Size(max = 50, message = "El estado de tratamiento es demasiado largo") private String treatmentStatus;
    private String specialty;
    private String skinType;
    private String knownAllergies;
    private String chronicConditions;
    private String sunExposureHabits;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
