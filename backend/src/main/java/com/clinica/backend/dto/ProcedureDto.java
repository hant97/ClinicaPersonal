package com.clinica.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureDto {
    private Long id;
    private Long patientId;
    @NotBlank(message = "El nombre del procedimiento es obligatorio") private String name;
    private String description;
    private LocalDate procedureDate;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
