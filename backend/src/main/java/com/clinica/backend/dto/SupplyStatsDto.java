package com.clinica.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SupplyStatsDto {
    private long totalSupplies;
    private long lowStockCount;
    private long outOfStockCount;
    private long expiringSoonCount;
    private BigDecimal inventoryValue;
}
