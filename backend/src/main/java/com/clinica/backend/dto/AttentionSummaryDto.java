package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttentionSummaryDto {
    private long totalToday;
    private long scheduledToday;
    private long inProgressToday;
    private long attendedToday;
    private long paidToday;
    private long cancelledToday;
    private BigDecimal pendingBillingAmount;
}
