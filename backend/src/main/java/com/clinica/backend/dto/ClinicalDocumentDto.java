package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalDocumentDto {
    private Long id;
    private Long patientId;
    private String specialty;
    private String category;
    private String name;
    private String mimeType;
    private Long sizeBytes;
    private LocalDate documentDate;
    private Long professionalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
