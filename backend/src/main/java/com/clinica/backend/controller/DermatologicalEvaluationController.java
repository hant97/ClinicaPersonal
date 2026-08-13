package com.clinica.backend.controller;

import com.clinica.backend.dto.DermatologicalEvaluationDto;
import com.clinica.backend.service.DermatologicalEvaluationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DermatologicalEvaluationController {

    private final DermatologicalEvaluationService service;

    @GetMapping("/patients/{patientId}/dermatological-evaluations")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Page<DermatologicalEvaluationDto>> getEvaluationsByPatientId(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getEvaluationsByPatientId(patientId, 
            PageRequest.of(page, size, Sort.by("evaluationDate").descending())));
    }

    @PostMapping("/patients/{patientId}/dermatological-evaluations")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<DermatologicalEvaluationDto> createEvaluation(
            @PathVariable Long patientId,
            @Valid @RequestBody DermatologicalEvaluationDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createEvaluation(dto));
    }

    @GetMapping("/dermatological-evaluations/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<DermatologicalEvaluationDto> getEvaluationById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getEvaluationById(id));
    }

    @PutMapping("/dermatological-evaluations/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<DermatologicalEvaluationDto> updateEvaluation(
            @PathVariable Long id,
            @Valid @RequestBody DermatologicalEvaluationDto dto) {
        return ResponseEntity.ok(service.updateEvaluation(id, dto));
    }

    @DeleteMapping("/dermatological-evaluations/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Void> deleteEvaluation(@PathVariable Long id) {
        service.deleteEvaluation(id);
        return ResponseEntity.noContent().build();
    }
}
