package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItemDto {
    private Long id;
    private Long catalogId;
    private String itemCode;
    private String itemName;
    private boolean isActive;
    private Integer orderIndex;
}
