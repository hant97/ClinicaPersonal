package com.clinica.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDto {
    private Long id;
    private Long patientId;
    private String specialty;
    private LocalDate prescriptionDate;
    private LocalDate validUntil;
    private String notes;
    private Long professionalId;
    private String verificationCode;
    private Long attentionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Valid
    @NotEmpty(message = "La receta debe incluir al menos un medicamento o indicación")
    private List<PrescriptionItemDto> items;
}
