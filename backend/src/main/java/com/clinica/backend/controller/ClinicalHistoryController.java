package com.clinica.backend.controller;

import com.clinica.backend.dto.ClinicalHistoryDto;
import com.clinica.backend.service.ClinicalHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClinicalHistoryController {

    private final ClinicalHistoryService service;

    @GetMapping("/patients/{patientId}/clinical-history")
    public ResponseEntity<ClinicalHistoryDto> getClinicalHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.getClinicalHistory(patientId));
    }
}
