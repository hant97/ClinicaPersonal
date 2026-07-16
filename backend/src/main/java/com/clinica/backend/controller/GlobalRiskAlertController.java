package com.clinica.backend.controller;

import com.clinica.backend.dto.RiskAlertDto;
import com.clinica.backend.service.RiskAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class GlobalRiskAlertController {

    @Autowired
    private RiskAlertService riskAlertService;

    @GetMapping("/active")
    public ResponseEntity<List<RiskAlertDto>> getAllActiveAlerts() {
        return ResponseEntity.ok(riskAlertService.getAllActiveAlerts());
    }
}
