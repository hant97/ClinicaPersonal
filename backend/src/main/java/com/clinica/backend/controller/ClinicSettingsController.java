package com.clinica.backend.controller;

import com.clinica.backend.dto.ClinicSettingsDto;
import com.clinica.backend.service.ClinicSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.clinica.backend.model.User;

@RestController
@RequestMapping("/api/v1/settings/clinic")
@RequiredArgsConstructor
public class ClinicSettingsController {

    private final ClinicSettingsService service;

    @GetMapping
    public ResponseEntity<ClinicSettingsDto> getSettings(Authentication authentication) {
        String specialty = getSpecialtyFromAuthentication(authentication);
        return ResponseEntity.ok(service.getSettings(specialty));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClinicSettingsDto> updateSettings(@Valid @RequestBody ClinicSettingsDto dto, Authentication authentication) {
        String specialty = getSpecialtyFromAuthentication(authentication);
        return ResponseEntity.ok(service.updateSettings(dto, specialty));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClinicSettingsDto> uploadLogo(@RequestParam("file") MultipartFile file, Authentication authentication) {
        String specialty = getSpecialtyFromAuthentication(authentication);
        return ResponseEntity.ok(service.uploadLogo(file, specialty));
    }

    @GetMapping("/logo/{filename:.+}")
    public ResponseEntity<Resource> serveLogo(@PathVariable String filename) {
        Resource resource = service.loadLogo(filename);
        String lower = filename.toLowerCase();
        String contentType = lower.endsWith(".png") ? "image/png"
                : lower.endsWith(".webp") ? "image/webp"
                : "image/jpeg";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'")
                .body(resource);
    }

    private String getSpecialtyFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getSpecialty();
        }
        return "PSICOLOGIA"; // Default
    }
}
