package com.clinica.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PsychologyEvaluationDto {
    private Long id;
    private Long patientId;
    @NotNull(message = "La fecha de evaluación es obligatoria") private LocalDate evaluationDate;
    private String initialEvaluation;
    private String psychologicalHistory;
    private String mentalExam;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
