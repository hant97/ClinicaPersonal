package com.clinica.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LesionPhotoDto {
    private Long id;
    private Long lesionId;
    private String fileUrl;
    @Size(max = 255, message = "La descripción es demasiado larga") private String description;
    private LocalDate takenDate;
    private LocalDateTime createdAt;
}
