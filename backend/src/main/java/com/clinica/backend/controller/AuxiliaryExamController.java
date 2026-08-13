package com.clinica.backend.controller;

import com.clinica.backend.dto.AuxiliaryExamDto;
import com.clinica.backend.service.AuxiliaryExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuxiliaryExamController {

    private final AuxiliaryExamService service;

    @GetMapping("/patients/{patientId}/auxiliary-exams")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Page<AuxiliaryExamDto>> getExams(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getExams(patientId, PageRequest.of(page, size)));
    }

    @PostMapping("/patients/{patientId}/auxiliary-exams")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<AuxiliaryExamDto> createExam(
            @PathVariable Long patientId,
            @Valid @RequestBody AuxiliaryExamDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createExam(patientId, dto));
    }

    @PutMapping("/auxiliary-exams/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<AuxiliaryExamDto> updateExam(@PathVariable Long id, @Valid @RequestBody AuxiliaryExamDto dto) {
        return ResponseEntity.ok(service.updateExam(id, dto));
    }

    @DeleteMapping("/auxiliary-exams/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        service.deleteExam(id);
        return ResponseEntity.noContent().build();
    }
}
