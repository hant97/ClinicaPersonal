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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<Page<AssessmentDto>> getAssessmentsByPatientId(
            @PathVariable Long patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(assessmentService.getAssessmentsByPatientId(patientId, pageable));
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
