package com.clinica.backend.controller;

import com.clinica.backend.dto.DashboardStatsDto;
import com.clinica.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.clinica.backend.model.User;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(Authentication authentication) {
        String specialty = getSpecialtyFromAuthentication(authentication);
        return ResponseEntity.ok(dashboardService.getDashboardStats(specialty));
    }

    private String getSpecialtyFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getSpecialty();
        }
        return "PSICOLOGIA"; // Default
    }
}
