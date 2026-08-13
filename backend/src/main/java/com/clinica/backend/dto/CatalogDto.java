package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogDto {
    private Long id;
    @NotBlank(message = "El código del catálogo es obligatorio") @Pattern(regexp = "[A-Z0-9_]+", message = "El código solo puede contener mayúsculas, números y guiones bajos") private String code;
    @NotBlank(message = "El nombre del catálogo es obligatorio") @Size(max = 100, message = "El nombre no debe superar 100 caracteres") private String name;
    private String description;
    private String specialty;
    @Valid private List<CatalogItemDto> items;
}
