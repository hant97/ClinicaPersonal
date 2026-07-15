package com.clinica.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssessmentDto {
    private Long id;
    private Long patientId;
    private Long psychometricTestId;
    private String testName;
    private LocalDateTime assessmentDate;
    private Integer totalScore;
    private String answersJson;
    private String notes;
}
