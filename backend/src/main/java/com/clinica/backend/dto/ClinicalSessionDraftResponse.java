package com.clinica.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ClinicalSessionDraftResponse {
    private JsonNode content;
    private LocalDateTime expiresAt;
    private Long version;
}
