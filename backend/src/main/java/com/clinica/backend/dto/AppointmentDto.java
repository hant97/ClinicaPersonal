package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private Long id;
    @NotNull(message = "El paciente es obligatorio") private Long patientId;
    private String patientName;
    @NotNull(message = "La fecha de cita es obligatoria") private LocalDate appointmentDate;
    @NotNull(message = "La hora de inicio es obligatoria") private LocalTime startTime;
    @NotNull(message = "La hora de fin es obligatoria") private LocalTime endTime;
    @NotBlank(message = "El estado es obligatorio") @Size(max = 50, message = "El estado es demasiado largo") private String status;
    @NotBlank(message = "La modalidad es obligatoria") @Size(max = 50, message = "La modalidad es demasiado larga") private String modality;
    private String videoCallLink;
    private Long professionalId;
    private boolean isFirstTime;
    private Long clinicalSessionId;
    private Long clinicalServiceId;
    private String clinicalServiceName;
    private String notes;
    private String specialty;
}
