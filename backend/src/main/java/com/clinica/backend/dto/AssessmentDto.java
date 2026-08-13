package com.clinica.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;

@Data
public class AssessmentDto {
    private Long id;
    @NotNull(message = "El paciente es obligatorio") private Long patientId;
    @NotNull(message = "La prueba psicométrica es obligatoria") private Long psychometricTestId;
    private String testName;
    private LocalDateTime assessmentDate;
    private Integer totalScore;
    private String answersJson;
    private String notes;
}
