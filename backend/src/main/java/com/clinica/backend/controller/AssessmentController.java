package com.clinica.backend.controller;

import com.clinica.backend.dto.AssessmentDto;
import com.clinica.backend.dto.PsychometricTestDto;
import com.clinica.backend.service.AssessmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Assuming CORS is handled globally, but keeping it safe
public class AssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    @GetMapping("/assessments/patient/{patientId}")
    public ResponseEntity<Page<AssessmentDto>> getAssessmentsByPatientId(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(assessmentService.getAssessmentsByPatientId(patientId, PageRequest.of(page, size)));
    }

    @PostMapping("/assessments")
    public ResponseEntity<AssessmentDto> saveAssessment(@RequestBody AssessmentDto dto) {
        return ResponseEntity.ok(assessmentService.saveAssessment(dto));
    }
}
