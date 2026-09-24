package com.clinica.backend.controller;

import com.clinica.backend.dto.LesionDto;
import com.clinica.backend.service.LesionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LesionController {

    private final LesionService service;

    @GetMapping("/patients/{patientId}/lesions")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Page<LesionDto>> getLesions(
            @PathVariable Long patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getLesions(patientId, pageable));
    }

    @PostMapping("/patients/{patientId}/lesions")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<LesionDto> createLesion(
            @PathVariable Long patientId,
            @Valid @RequestBody LesionDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createLesion(patientId, dto));
    }

    @PutMapping("/lesions/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<LesionDto> updateLesion(@PathVariable Long id, @Valid @RequestBody LesionDto dto) {
        return ResponseEntity.ok(service.updateLesion(id, dto));
    }

    @DeleteMapping("/lesions/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Void> deleteLesion(@PathVariable Long id) {
        service.deleteLesion(id);
        return ResponseEntity.noContent().build();
    }
}
