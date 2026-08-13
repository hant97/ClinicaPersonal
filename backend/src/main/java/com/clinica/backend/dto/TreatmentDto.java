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
public class TreatmentDto {
    private Long id;
    private Long patientId;
    @NotBlank(message = "El nombre del tratamiento es obligatorio") private String name;
    @Size(max = 100, message = "La dosis es demasiado larga") private String dose;
    @Size(max = 100, message = "La vía es demasiado larga") private String route;
    @Size(max = 100, message = "La frecuencia es demasiado larga") private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    @Size(max = 50, message = "El estado es demasiado largo") private String status;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
