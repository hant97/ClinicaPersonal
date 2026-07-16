package com.clinica.backend.controller;

import com.clinica.backend.dto.RiskAlertDto;
import com.clinica.backend.service.RiskAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/patients/{patientId}/alerts")
public class RiskAlertController {

    @Autowired
    private RiskAlertService riskAlertService;

    @GetMapping
    public ResponseEntity<Page<RiskAlertDto>> getAlerts(
            @PathVariable Long patientId,
            @RequestParam(required = false, defaultValue = "false") boolean onlyActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity
                .ok(riskAlertService.getAlertsByPatientId(patientId, onlyActive, PageRequest.of(page, size)));
    }

    @PostMapping
    public ResponseEntity<RiskAlertDto> createAlert(
            @PathVariable Long patientId,
            @RequestBody RiskAlertDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(riskAlertService.createAlert(dto));
    }

    @PutMapping("/{alertId}/resolve")
    public ResponseEntity<RiskAlertDto> resolveAlert(@PathVariable Long alertId) {
        return ResponseEntity.ok(riskAlertService.resolveAlert(alertId));
    }
}
