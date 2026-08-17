package com.clinica.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ClinicalServiceStatsDto {
    private long totalServices;
    private long activeCount;
    private BigDecimal averagePrice;
    private List<ServicePerformance> topByRevenue;
    private List<ServicePerformance> topByQuantity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicePerformance {
        private Long serviceId;
        private String name;
        private long quantity;
        private BigDecimal total;
    }
}
