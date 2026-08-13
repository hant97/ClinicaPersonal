package com.clinica.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAppointmentStatusRequest {

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 50, message = "El estado es demasiado largo")
    private String status;
}
