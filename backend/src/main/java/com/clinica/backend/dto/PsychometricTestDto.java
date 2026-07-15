package com.clinica.backend.dto;

import lombok.Data;

@Data
public class PsychometricTestDto {
    private Long id;
    private String name;
    private String description;
    private String questionsJson;
}
