package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalSessionDto {
    private Long id;
    private Long patientId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String sessionType;
    private String modality;
    private String status;
    private String subjective;
    private String objective;
    private String analysis;
    private String plan;
    private boolean isConfidential;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long professionalId;
    private Long appointmentId;
}
