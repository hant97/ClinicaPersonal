package com.clinica.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttentionDto {

    private Long id;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long patientId;

    private String patientName;
    private String patientDocumentNumber;

    private Long professionalId;
    private String professionalName;

    private Long appointmentId;
    private Long clinicalSessionId;
    private Long prescriptionId;
    private Long paymentId;
    private String paymentStatus;
    private BigDecimal paymentAmount;

    private Long clinicalServiceId;
    private String clinicalServiceName;

    private String specialty;

    @NotNull(message = "La fecha de atención es obligatoria")
    private LocalDate attentionDate;

    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;

    private String status; // AGENDADA, EN_PROCESO, ATENDIDA, COBRADA, CANCELADA

    private String motive;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
