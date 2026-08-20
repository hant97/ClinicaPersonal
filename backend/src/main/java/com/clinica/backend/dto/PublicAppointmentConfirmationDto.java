package com.clinica.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicAppointmentConfirmationDto {
    private boolean confirmed;
    private boolean alreadyConfirmed;
    private String message;
    private String patientName;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private String modality;
    private String clinicName;
}
