package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalSessionDto {
    private Long id;
    @NotNull(message = "El paciente es obligatorio") private Long patientId;
    @NotNull(message = "La fecha de sesión es obligatoria") private LocalDate sessionDate;
    @NotNull(message = "La hora de inicio es obligatoria") private LocalTime startTime;
    @NotNull(message = "La hora de fin es obligatoria") private LocalTime endTime;
    @NotBlank(message = "El tipo de sesión es obligatorio") @Size(max = 100, message = "El tipo de sesión es demasiado largo") private String sessionType;
    @NotBlank(message = "La modalidad es obligatoria") @Size(max = 50, message = "La modalidad es demasiado larga") private String modality;
    @NotBlank(message = "El estado es obligatorio") @Size(max = 50, message = "El estado es demasiado largo") private String status;
    private String subjective;
    private String objective;
    private String analysis;
    private String plan;
    private boolean isConfidential;
    private String specialty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long professionalId;
    private Long appointmentId;
}
