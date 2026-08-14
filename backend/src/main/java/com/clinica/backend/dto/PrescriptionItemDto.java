package com.clinica.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemDto {
    private Long id;
    @NotBlank(message = "El nombre del medicamento o indicación es obligatorio")
    private String name;
    @Size(max = 100, message = "La dosis es demasiado larga")
    private String dose;
    @Size(max = 100, message = "La frecuencia es demasiado larga")
    private String frequency;
    @Size(max = 100, message = "La duración es demasiado larga")
    private String duration;
    @Size(max = 100, message = "La vía es demasiado larga")
    private String route;
    private String instructions;
}
