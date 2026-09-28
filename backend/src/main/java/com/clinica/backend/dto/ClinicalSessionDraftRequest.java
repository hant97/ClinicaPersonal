package com.clinica.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

@Getter
@Setter
@NoArgsConstructor
public class ClinicalSessionDraftRequest {

    @NotNull
    private JsonNode content;
}
