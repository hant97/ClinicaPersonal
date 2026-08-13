package com.clinica.backend.controller;

import com.clinica.backend.dto.DiagnosisDto;
import com.clinica.backend.service.DiagnosisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService service;

    @GetMapping("/patients/{patientId}/diagnoses")
    public ResponseEntity<Page<DiagnosisDto>> getDiagnoses(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getDiagnoses(patientId, PageRequest.of(page, size)));
    }

    @PostMapping("/patients/{patientId}/diagnoses")
    public ResponseEntity<DiagnosisDto> createDiagnosis(
            @PathVariable Long patientId,
            @Valid @RequestBody DiagnosisDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createDiagnosis(patientId, dto));
    }

    @PutMapping("/diagnoses/{id}")
    public ResponseEntity<DiagnosisDto> updateDiagnosis(@PathVariable Long id, @Valid @RequestBody DiagnosisDto dto) {
        return ResponseEntity.ok(service.updateDiagnosis(id, dto));
    }

    @DeleteMapping("/diagnoses/{id}")
    public ResponseEntity<Void> deleteDiagnosis(@PathVariable Long id) {
        service.deleteDiagnosis(id);
        return ResponseEntity.noContent().build();
    }
}
