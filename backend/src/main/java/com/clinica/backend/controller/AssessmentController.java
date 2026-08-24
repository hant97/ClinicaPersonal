package com.clinica.backend.controller;

import com.clinica.backend.dto.AssessmentDto;
import com.clinica.backend.dto.PsychometricTestDto;
import com.clinica.backend.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<Page<AssessmentDto>> getAssessmentsByPatientId(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(assessmentService.getAssessmentsByPatientId(patientId, PageRequest.of(page, size)));
    }

    @GetMapping("/patient/{patientId}/evolution")
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<List<AssessmentDto>> getPatientEvolution(@PathVariable Long patientId) {
        return ResponseEntity.ok(assessmentService.getPatientEvolution(patientId));
    }

    @PostMapping
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<AssessmentDto> saveAssessment(@Valid @RequestBody AssessmentDto dto) {
        return ResponseEntity.ok(assessmentService.saveAssessment(dto));
    }
}
