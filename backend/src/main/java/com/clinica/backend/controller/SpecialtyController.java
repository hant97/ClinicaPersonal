package com.clinica.backend.controller;

import com.clinica.backend.dto.SpecialtyDto;
import com.clinica.backend.service.SpecialtyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
@RequiredArgsConstructor
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    @GetMapping
    public ResponseEntity<List<SpecialtyDto>> getActiveSpecialties() {
        return ResponseEntity.ok(specialtyService.getActiveSpecialties());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<List<SpecialtyDto>> getAllSpecialties() {
        return ResponseEntity.ok(specialtyService.getAllSpecialties());
    }

    @GetMapping("/{code}")
    public ResponseEntity<SpecialtyDto> getSpecialtyByCode(@PathVariable String code) {
        return ResponseEntity.ok(specialtyService.getSpecialtyByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<SpecialtyDto> createSpecialty(@Valid @RequestBody SpecialtyDto dto) {
        SpecialtyDto created = specialtyService.createSpecialty(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<SpecialtyDto> updateSpecialty(@PathVariable Long id, @Valid @RequestBody SpecialtyDto dto) {
        return ResponseEntity.ok(specialtyService.updateSpecialty(id, dto));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('SITE_ADMIN')")
    public ResponseEntity<SpecialtyDto> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(specialtyService.toggleActive(id, active));
    }
}
