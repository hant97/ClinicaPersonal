package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalServiceDto {
    private Long id;
    @NotBlank(message = "El nombre del servicio es obligatorio") @Size(max = 150, message = "El nombre no debe superar 150 caracteres") private String name;
    private String description;
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo") private BigDecimal price;
    private String specialty;
}
