package com.clinica.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PaymentSummaryDto {
    private BigDecimal incomeToday;
    private BigDecimal incomeMonth;
    private int monthlyGrowth;
    private long paymentsCountMonth;
    private BigDecimal averageTicket;
    private List<MethodSummary> methodBreakdown;
    private List<DailyIncome> dailyIncome;
    private List<ServiceSummary> topServices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodSummary {
        private String method;
        private BigDecimal total;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyIncome {
        private LocalDate date;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceSummary {
        private String name;
        private long quantity;
        private BigDecimal total;
    }
}
