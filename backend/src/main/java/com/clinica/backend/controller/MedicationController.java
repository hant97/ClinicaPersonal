package com.clinica.backend.controller;

import com.clinica.backend.dto.MedicationDto;
import com.clinica.backend.service.MedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService service;

    @GetMapping("/patients/{patientId}/medications")
    public ResponseEntity<Page<MedicationDto>> getMedications(
            @PathVariable Long patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getMedications(patientId, pageable));
    }

    @PostMapping("/patients/{patientId}/medications")
    public ResponseEntity<MedicationDto> createMedication(
            @PathVariable Long patientId,
            @Valid @RequestBody MedicationDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createMedication(patientId, dto));
    }

    @PutMapping("/medications/{id}")
    public ResponseEntity<MedicationDto> updateMedication(@PathVariable Long id, @Valid @RequestBody MedicationDto dto) {
        return ResponseEntity.ok(service.updateMedication(id, dto));
    }

    @DeleteMapping("/medications/{id}")
    public ResponseEntity<Void> deleteMedication(@PathVariable Long id) {
        service.deleteMedication(id);
        return ResponseEntity.noContent().build();
    }
}
