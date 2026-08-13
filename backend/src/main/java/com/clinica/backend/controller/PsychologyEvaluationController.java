package com.clinica.backend.controller;

import com.clinica.backend.dto.PsychologyEvaluationDto;
import com.clinica.backend.service.PsychologyEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PsychologyEvaluationController {

    private final PsychologyEvaluationService service;

    @GetMapping("/patients/{patientId}/psychology-evaluations")
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<Page<PsychologyEvaluationDto>> getEvaluationsByPatientId(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getEvaluationsByPatientId(patientId,
                PageRequest.of(page, size, Sort.by("evaluationDate").descending())));
    }

    @PostMapping("/patients/{patientId}/psychology-evaluations")
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<PsychologyEvaluationDto> createEvaluation(
            @PathVariable Long patientId,
            @Valid @RequestBody PsychologyEvaluationDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createEvaluation(dto));
    }

    @GetMapping("/psychology-evaluations/{id}")
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<PsychologyEvaluationDto> getEvaluationById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getEvaluationById(id));
    }

    @PutMapping("/psychology-evaluations/{id}")
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<PsychologyEvaluationDto> updateEvaluation(
            @PathVariable Long id,
            @Valid @RequestBody PsychologyEvaluationDto dto) {
        return ResponseEntity.ok(service.updateEvaluation(id, dto));
    }

    @DeleteMapping("/psychology-evaluations/{id}")
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<Void> deleteEvaluation(@PathVariable Long id) {
        service.deleteEvaluation(id);
        return ResponseEntity.noContent().build();
    }
}
