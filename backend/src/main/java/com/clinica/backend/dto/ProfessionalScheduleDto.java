package com.clinica.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfessionalScheduleDto {
    private Long id;
    private Long professionalId;

    @NotNull(message = "El día de la semana es obligatorio")
    @Min(value = 1, message = "El día de la semana debe ser entre 1 (Lunes) y 7 (Domingo)")
    @Max(value = 7, message = "El día de la semana debe ser entre 1 (Lunes) y 7 (Domingo)")
    private Integer dayOfWeek;

    @NotNull(message = "La hora de inicio es obligatoria")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm[:ss]")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm[:ss]")
    private LocalTime endTime;

    private boolean active = true;
    private String specialty;
}
