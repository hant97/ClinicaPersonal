package com.clinica.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class PsychometricTestDto {
    private Long id;
    @NotBlank(message = "El nombre de la prueba es obligatorio") @Size(max = 150, message = "El nombre es demasiado largo") private String name;
    private String description;
    @NotBlank(message = "Las preguntas son obligatorias") private String questionsJson;
    private String interpretationJson;
    private long usageCount;
    private LocalDateTime lastUsedAt;
}
