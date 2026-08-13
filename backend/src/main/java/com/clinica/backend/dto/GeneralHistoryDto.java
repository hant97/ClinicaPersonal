package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralHistoryDto {
    private Long id;
    private Long patientId;
    private String specialty;
    private String pathologicalHistory;
    private String surgicalHistory;
    private String familyHistory;
    private String habits;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
