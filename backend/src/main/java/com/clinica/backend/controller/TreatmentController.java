package com.clinica.backend.controller;

import com.clinica.backend.dto.TreatmentDto;
import com.clinica.backend.service.TreatmentService;
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
public class TreatmentController {

    private final TreatmentService service;

    @GetMapping("/patients/{patientId}/treatments")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Page<TreatmentDto>> getTreatments(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getTreatments(patientId, PageRequest.of(page, size)));
    }

    @PostMapping("/patients/{patientId}/treatments")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<TreatmentDto> createTreatment(
            @PathVariable Long patientId,
            @Valid @RequestBody TreatmentDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createTreatment(patientId, dto));
    }

    @PutMapping("/treatments/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<TreatmentDto> updateTreatment(@PathVariable Long id, @Valid @RequestBody TreatmentDto dto) {
        return ResponseEntity.ok(service.updateTreatment(id, dto));
    }

    @DeleteMapping("/treatments/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Void> deleteTreatment(@PathVariable Long id) {
        service.deleteTreatment(id);
        return ResponseEntity.noContent().build();
    }
}
