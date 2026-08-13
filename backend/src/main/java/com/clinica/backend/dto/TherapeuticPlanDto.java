package com.clinica.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TherapeuticPlanDto {
    private Long id;
    private Long patientId;
    private String specialty;
    private String objectives;
    private String interventions;
    private LocalDate startDate;
    private LocalDate endDate;
    @Size(max = 50, message = "El estado es demasiado largo") private String status;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
