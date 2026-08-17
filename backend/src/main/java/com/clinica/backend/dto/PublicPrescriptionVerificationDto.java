package com.clinica.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicPrescriptionVerificationDto {
    private String verificationCode;
    private String patientName;
    private String patientIdentificationDocument;
    private LocalDate prescriptionDate;
    private LocalDate validUntil;
    private boolean valid;
    private String statusMessage;
    private String professionalName;
    private String specialty;
    private String clinicName;
    private String notes;
    private List<PrescriptionItemDto> items;
}
