package com.clinica.backend.controller;

import com.clinica.backend.dto.DermatologicalHistoryDto;
import com.clinica.backend.service.DermatologicalHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DermatologicalHistoryController {

    private final DermatologicalHistoryService service;

    @GetMapping("/patients/{patientId}/dermatological-history")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<DermatologicalHistoryDto> getHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.getHistory(patientId));
    }

    @PutMapping("/patients/{patientId}/dermatological-history")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<DermatologicalHistoryDto> upsertHistory(
            @PathVariable Long patientId,
            @Valid @RequestBody DermatologicalHistoryDto dto) {
        return ResponseEntity.ok(service.upsertHistory(patientId, dto));
    }
}
