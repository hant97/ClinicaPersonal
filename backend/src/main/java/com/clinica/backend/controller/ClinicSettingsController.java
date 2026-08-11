package com.clinica.backend.controller;

import com.clinica.backend.dto.ClinicSettingsDto;
import com.clinica.backend.service.ClinicSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping({"/api/v1/settings/clinic", "/api/settings/clinic"})
@RequiredArgsConstructor
public class ClinicSettingsController {

    private final ClinicSettingsService service;

    @GetMapping
    public ResponseEntity<ClinicSettingsDto> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClinicSettingsDto> updateSettings(@RequestBody ClinicSettingsDto dto) {
        return ResponseEntity.ok(service.updateSettings(dto));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClinicSettingsDto> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadLogo(file));
    }
    
    @GetMapping("/logo/{filename:.+}")
    public ResponseEntity<Resource> serveLogo(@PathVariable String filename) {
        try {
            if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path basePath = Paths.get("uploads/logos/").toAbsolutePath().normalize();
            Path filePath = basePath.resolve(filename).normalize();
            
            if (!filePath.startsWith(basePath)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                String contentType = "image/jpeg";
                String lower = filename.toLowerCase();
                if (lower.endsWith(".png")) {
                    contentType = "image/png";
                } else if (lower.endsWith(".svg")) {
                    contentType = "image/svg+xml";
                } else if (lower.endsWith(".webp")) {
                    contentType = "image/webp";
                }
                
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
