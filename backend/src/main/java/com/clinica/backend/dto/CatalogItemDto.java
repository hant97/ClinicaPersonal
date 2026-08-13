package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItemDto {
    private Long id;
    private Long catalogId;
    @NotBlank(message = "El código del ítem es obligatorio") @Size(max = 100, message = "El código del ítem es demasiado largo") private String itemCode;
    @NotBlank(message = "El nombre del ítem es obligatorio") @Size(max = 150, message = "El nombre del ítem es demasiado largo") private String itemName;
    @JsonProperty("isActive")
    private boolean active;
    @PositiveOrZero(message = "El orden no puede ser negativo") private Integer orderIndex;
}
