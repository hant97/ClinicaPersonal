package com.clinica.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRiskAlertDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String type;
    private String level;
    private String description;
    private boolean active;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
