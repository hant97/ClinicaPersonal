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
public class MedicationDto {
    private Long id;
    private Long patientId;
    private String specialty;
    @NotBlank(message = "El nombre del medicamento es obligatorio") private String name;
    @Size(max = 100, message = "La dosis es demasiado larga") private String dose;
    @Size(max = 100, message = "La frecuencia es demasiado larga") private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active = true;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
