package com.clinica.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeeklyScheduleDto {
    @NotNull(message = "El profesional es obligatorio")
    private Long professionalId;
    private String professionalName;
    private String specialty;

    @Valid
    private List<ProfessionalScheduleDto> schedules = new ArrayList<>();
}
