package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DermatologicalHistoryDto {
    private Long id;
    private Long patientId;
    private String skinType;
    private String sunExposureHabits;
    private String personalSkinHistory;
    private String familySkinHistory;
    private String chronicConditions;
    private String examFindings;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
