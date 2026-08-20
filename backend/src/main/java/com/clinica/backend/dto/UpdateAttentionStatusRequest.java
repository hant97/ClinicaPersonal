package com.clinica.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAttentionStatusRequest {

    @NotBlank(message = "El nuevo estado es obligatorio")
    private String status;

    private String notes;
}
