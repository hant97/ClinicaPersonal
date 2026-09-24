package com.clinica.backend.controller;

import com.clinica.backend.dto.RiskAlertDto;
import com.clinica.backend.service.RiskAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/alerts")
@RequiredArgsConstructor
public class RiskAlertController {

    private final RiskAlertService riskAlertService;

    @GetMapping
    public ResponseEntity<Page<RiskAlertDto>> getAlerts(
            @PathVariable Long patientId,
            @RequestParam(required = false, defaultValue = "false") boolean onlyActive,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity
                .ok(riskAlertService.getAlertsByPatientId(patientId, onlyActive, pageable));
    }

    @PostMapping
    public ResponseEntity<RiskAlertDto> createAlert(
            @PathVariable Long patientId,
            @Valid @RequestBody RiskAlertDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(riskAlertService.createAlert(dto));
    }

    @PutMapping("/{alertId}/resolve")
    public ResponseEntity<RiskAlertDto> resolveAlert(@PathVariable Long alertId) {
        return ResponseEntity.ok(riskAlertService.resolveAlert(alertId));
    }
}
