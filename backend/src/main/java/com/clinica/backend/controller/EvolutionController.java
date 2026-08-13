package com.clinica.backend.controller;

import com.clinica.backend.dto.EvolutionDto;
import com.clinica.backend.service.EvolutionService;
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
public class EvolutionController {

    private final EvolutionService service;

    @GetMapping("/patients/{patientId}/evolutions")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Page<EvolutionDto>> getEvolutions(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getEvolutions(patientId, PageRequest.of(page, size)));
    }

    @PostMapping("/patients/{patientId}/evolutions")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<EvolutionDto> createEvolution(
            @PathVariable Long patientId,
            @Valid @RequestBody EvolutionDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createEvolution(patientId, dto));
    }

    @PutMapping("/evolutions/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<EvolutionDto> updateEvolution(@PathVariable Long id, @Valid @RequestBody EvolutionDto dto) {
        return ResponseEntity.ok(service.updateEvolution(id, dto));
    }

    @DeleteMapping("/evolutions/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Void> deleteEvolution(@PathVariable Long id) {
        service.deleteEvolution(id);
        return ResponseEntity.noContent().build();
    }
}
