package com.clinica.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RiskAlertDto {
    private Long id;
    private Long patientId;
    private String type;
    private String level;
    private String description;
    private boolean active;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
