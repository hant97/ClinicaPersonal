package com.clinica.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RiskAlertDto {
    private Long id;
    private Long patientId;
    private String specialty;
    @NotBlank(message = "El tipo de alerta es obligatorio") @Size(max = 50, message = "El tipo de alerta es demasiado largo") private String type;
    @NotBlank(message = "El nivel de alerta es obligatorio") @Size(max = 50, message = "El nivel de alerta es demasiado largo") private String level;
    @NotBlank(message = "La descripción es obligatoria") @Size(max = 1000, message = "La descripción es demasiado larga") private String description;
    private boolean active;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
