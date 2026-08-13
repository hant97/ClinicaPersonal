package com.clinica.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuxiliaryExamDto {
    private Long id;
    private Long patientId;
    @Size(max = 100, message = "El tipo de examen es demasiado largo") private String examType;
    private String description;
    private String result;
    private LocalDate examDate;
    private String notes;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
