package com.clinica.backend.controller;

import com.clinica.backend.dto.RiskAlertDto;
import com.clinica.backend.service.RiskAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/alerts")
public class GlobalRiskAlertController {

    @Autowired
    private RiskAlertService riskAlertService;

    @GetMapping("/active")
    public ResponseEntity<Page<RiskAlertDto>> getAllActiveAlerts(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(riskAlertService.getAllActiveAlerts(pageable));
    }
}
