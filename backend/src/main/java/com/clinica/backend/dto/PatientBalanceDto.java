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
public class PatientBalanceDto {
    private Long patientId;
    private BigDecimal totalCharged;
    private BigDecimal totalPaid;
    private BigDecimal balance;
}
