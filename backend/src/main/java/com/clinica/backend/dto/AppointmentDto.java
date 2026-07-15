package com.clinica.backend.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class AppointmentDto {
    private Long id;
    private Long patientId;
    private LocalDateTime appointmentDate;
    private String status;
    private String notes;
}
