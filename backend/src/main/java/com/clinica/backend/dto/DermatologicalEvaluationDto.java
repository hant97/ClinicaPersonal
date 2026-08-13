package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DermatologicalEvaluationDto {
    private Long id;
    private Long patientId;
    @NotNull(message = "La fecha de evaluación es obligatoria") private LocalDate evaluationDate;
    @Size(max = 100, message = "El tipo de piel es demasiado largo") private String skinType;
    @Size(max = 100, message = "El área afectada es demasiado larga") private String affectedArea;
    @Size(max = 100, message = "El tipo de lesión es demasiado largo") private String lesionType;
    @Size(max = 50, message = "El tamaño de lesión es demasiado largo") private String lesionSize;
    private String dermatologicalDiagnosis;
    private String treatmentIndicated;
    private String procedurePerformed;
    private String evolutionNotes;
    private LocalDate nextReviewDate;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
