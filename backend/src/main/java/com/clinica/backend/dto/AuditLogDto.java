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
public class AuditLogDto {
    private Long id;
    private Long userId;
    private String username;
    private String specialty;
    private String action;
    private String entityType;
    private String entityId;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
