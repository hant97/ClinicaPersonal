package com.clinica.backend.controller;

import com.clinica.backend.dto.ProcedureDto;
import com.clinica.backend.service.ProcedureService;
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
public class ProcedureController {

    private final ProcedureService service;

    @GetMapping("/patients/{patientId}/procedures")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Page<ProcedureDto>> getProcedures(
            @PathVariable Long patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getProcedures(patientId, pageable));
    }

    @PostMapping("/patients/{patientId}/procedures")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<ProcedureDto> createProcedure(
            @PathVariable Long patientId,
            @Valid @RequestBody ProcedureDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createProcedure(patientId, dto));
    }

    @PutMapping("/procedures/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<ProcedureDto> updateProcedure(@PathVariable Long id, @Valid @RequestBody ProcedureDto dto) {
        return ResponseEntity.ok(service.updateProcedure(id, dto));
    }

    @DeleteMapping("/procedures/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Void> deleteProcedure(@PathVariable Long id) {
        service.deleteProcedure(id);
        return ResponseEntity.noContent().build();
    }
}
