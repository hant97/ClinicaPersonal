package com.clinica.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalProductivityDto {
    private Long professionalId;
    private String professionalName;
    private long totalAttentions;
    private long attendedAttentions;
    private long cancelledAttentions;
    private long paidAttentions;
    private int completionRate;
    private BigDecimal billedAmount;
    private BigDecimal collectedAmount;
}
