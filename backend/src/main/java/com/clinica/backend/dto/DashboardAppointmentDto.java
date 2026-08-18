package com.clinica.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class DashboardAppointmentDto {
    private Long id;
    private Long patientId;
    private String patientUuid;
    private String patientName;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String modality;
    private String videoCallLink;
    @JsonProperty("isFirstTime")
    private boolean isFirstTime;
    private String notes;
    @JsonProperty("isPaid")
    private boolean isPaid;
    private Long paymentId;
    private java.math.BigDecimal paymentAmount;
}
