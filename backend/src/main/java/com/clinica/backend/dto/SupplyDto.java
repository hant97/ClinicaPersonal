package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplyDto {
    private Long id;
    private String name;
    private String description;
    private Integer currentStock;
    private Integer minStockLevel;
    private String unit;
    private LocalDate expirationDate;
}
