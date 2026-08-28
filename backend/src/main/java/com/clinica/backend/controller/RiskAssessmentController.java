package com.clinica.backend.controller;

import com.clinica.backend.dto.RiskAssessmentDto;
import com.clinica.backend.service.RiskAssessmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

/**
 * Lectura del historial de evaluaciones de riesgo de un paciente. La creación
 * y actualización siempre ocurren de forma transaccional a través de
 * {@link ClinicalSessionController}, ligadas a la sesión clínica que las origina.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RiskAssessmentController {

    private final RiskAssessmentService service;

    @GetMapping("/patients/{patientId}/risk-assessments")
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<Page<RiskAssessmentDto>> getHistoryByPatientId(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getHistoryByPatientId(patientId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }
}
