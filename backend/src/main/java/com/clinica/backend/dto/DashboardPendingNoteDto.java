package com.clinica.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPendingNoteDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private LocalDate sessionDate;
    private String sessionType;
}
