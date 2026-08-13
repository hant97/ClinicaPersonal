package com.clinica.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisDto {
    private Long id;
    private Long patientId;
    private String specialty;
    @Size(max = 100, message = "La categoría es demasiado larga") private String category;
    @NotBlank(message = "La descripción del diagnóstico es obligatoria") private String description;
    @Size(max = 50, message = "El estado es demasiado largo") private String status;
    private LocalDate diagnosisDate;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
