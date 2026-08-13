package com.clinica.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllergyDto {
    private Long id;
    private Long patientId;
    private String specialty;
    @NotBlank(message = "El alérgeno es obligatorio") private String allergen;
    @Size(max = 50, message = "El tipo es demasiado largo") private String type;
    @Size(max = 50, message = "La severidad es demasiado larga") private String severity;
    private String reaction;
    private boolean active = true;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
