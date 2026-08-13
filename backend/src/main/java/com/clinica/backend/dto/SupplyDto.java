package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplyDto {
    private Long id;
    @NotBlank(message = "El nombre del insumo es obligatorio") @Size(max = 150, message = "El nombre no debe superar 150 caracteres") private String name;
    private String description;
    @NotNull(message = "El stock actual es obligatorio") @PositiveOrZero(message = "El stock actual no puede ser negativo") private Integer currentStock;
    @NotNull(message = "El stock mínimo es obligatorio") @PositiveOrZero(message = "El stock mínimo no puede ser negativo") private Integer minStockLevel;
    @NotBlank(message = "La unidad es obligatoria") @Size(max = 50, message = "La unidad es demasiado larga") private String unit;
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo") private BigDecimal price;
    private LocalDate expirationDate;
    private String specialty;
    @Size(max = 500, message = "La URL de la imagen es demasiado larga") private String imageUrl;
}
