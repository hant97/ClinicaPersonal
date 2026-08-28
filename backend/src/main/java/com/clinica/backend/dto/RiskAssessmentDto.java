package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentDto {
    private Long id;
    private Long patientId;
    private Long clinicalSessionId;
    private Long professionalId;
    @NotNull(message = "Debe indicarse si hay ideación suicida") private Boolean suicidalIdeation;
    @Size(max = 100, message = "La frecuencia de ideación es demasiado larga") private String ideationFrequency;
    private Boolean hasPlan;
    private String planDescription;
    private Boolean meansAccess;
    private String meansDescription;
    private String protectiveFactors;
    @NotNull(message = "El nivel de riesgo es obligatorio") @Size(max = 50, message = "El nivel de riesgo es demasiado largo") private String riskLevel;
    private String actionTaken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
