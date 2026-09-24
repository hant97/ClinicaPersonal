package com.clinica.backend.controller;

import com.clinica.backend.dto.AllergyDto;
import com.clinica.backend.service.AllergyService;
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
public class AllergyController {

    private final AllergyService service;

    @GetMapping("/patients/{patientId}/allergies")
    public ResponseEntity<Page<AllergyDto>> getAllergies(
            @PathVariable Long patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getAllergies(patientId, pageable));
    }

    @PostMapping("/patients/{patientId}/allergies")
    public ResponseEntity<AllergyDto> createAllergy(
            @PathVariable Long patientId,
            @Valid @RequestBody AllergyDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createAllergy(patientId, dto));
    }

    @PutMapping("/allergies/{id}")
    public ResponseEntity<AllergyDto> updateAllergy(@PathVariable Long id, @Valid @RequestBody AllergyDto dto) {
        return ResponseEntity.ok(service.updateAllergy(id, dto));
    }

    @DeleteMapping("/allergies/{id}")
    public ResponseEntity<Void> deleteAllergy(@PathVariable Long id) {
        service.deleteAllergy(id);
        return ResponseEntity.noContent().build();
    }
}
