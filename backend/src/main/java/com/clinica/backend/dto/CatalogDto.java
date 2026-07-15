package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private List<CatalogItemDto> items;
}
