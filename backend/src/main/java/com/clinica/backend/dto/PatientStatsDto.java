package com.clinica.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatientStatsDto {
    private long totalPatients;
    private long newThisMonth;
    private long withActiveAlerts;
    private long minors;
}
