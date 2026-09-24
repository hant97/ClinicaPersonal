package com.clinica.backend.controller;

import com.clinica.backend.dto.PrescriptionDto;
import com.clinica.backend.service.PrescriptionService;
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
public class PrescriptionController {

    private final PrescriptionService service;

    @GetMapping("/patients/{patientId}/prescriptions")
    public ResponseEntity<Page<PrescriptionDto>> getPrescriptions(
            @PathVariable Long patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getPrescriptions(patientId, pageable));
    }

    @PostMapping("/patients/{patientId}/prescriptions")
    public ResponseEntity<PrescriptionDto> createPrescription(
            @PathVariable Long patientId,
            @Valid @RequestBody PrescriptionDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createPrescription(patientId, dto));
    }

    @PutMapping("/prescriptions/{id}")
    public ResponseEntity<PrescriptionDto> updatePrescription(@PathVariable Long id, @Valid @RequestBody PrescriptionDto dto) {
        return ResponseEntity.ok(service.updatePrescription(id, dto));
    }

    @DeleteMapping("/prescriptions/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        service.deletePrescription(id);
        return ResponseEntity.noContent().build();
    }
}
