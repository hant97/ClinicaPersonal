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
public class LesionDto {
    private Long id;
    private Long patientId;
    @Size(max = 100, message = "La zona del cuerpo es demasiado larga") private String bodyArea;
    @Size(max = 100, message = "El tipo de lesión es demasiado largo") private String lesionType;
    @Size(max = 50, message = "El tamaño es demasiado largo") private String size;
    @Size(max = 255, message = "La morfología es demasiado larga") private String morphology;
    @Size(max = 100, message = "El color es demasiado largo") private String color;
    private LocalDate sinceDate;
    private String evolution;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
