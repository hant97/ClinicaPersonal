package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItemDto {
    private Long id;
    private Long catalogId;
    private String itemCode;
    private String itemName;
    @JsonProperty("isActive")
    private boolean active;
    private Integer orderIndex;
}
