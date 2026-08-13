package com.clinica.backend.controller;

import com.clinica.backend.dto.GeneralHistoryDto;
import com.clinica.backend.service.GeneralHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GeneralHistoryController {

    private final GeneralHistoryService service;

    @GetMapping("/patients/{patientId}/general-history")
    public ResponseEntity<GeneralHistoryDto> getGeneralHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.getGeneralHistory(patientId));
    }

    @PutMapping("/patients/{patientId}/general-history")
    public ResponseEntity<GeneralHistoryDto> upsertGeneralHistory(
            @PathVariable Long patientId,
            @Valid @RequestBody GeneralHistoryDto dto) {
        return ResponseEntity.ok(service.upsertGeneralHistory(patientId, dto));
    }
}
