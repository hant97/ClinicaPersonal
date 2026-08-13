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
public class EvolutionDto {
    private Long id;
    private Long patientId;
    @NotNull(message = "La fecha de control es obligatoria") private LocalDate controlDate;
    private String clinicalNotes;
    private LocalDate nextControlDate;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
