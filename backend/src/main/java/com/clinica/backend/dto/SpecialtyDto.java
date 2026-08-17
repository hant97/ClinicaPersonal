package com.clinica.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyDto {

    private Long id;

    @NotBlank(message = "El código de la especialidad es obligatorio")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "El código solo puede contener letras mayúsculas, números y guiones bajos")
    @Size(max = 50, message = "El código no debe superar 50 caracteres")
    private String code;

    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
    private String name;

    @Size(max = 255, message = "La descripción no debe superar 255 caracteres")
    private String description;

    @Size(max = 50, message = "El icono no debe superar 50 caracteres")
    private String icon = "Sparkles";

    private boolean active = true;

    private int displayOrder = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
