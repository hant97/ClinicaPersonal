package com.clinica.backend.controller;

import com.clinica.backend.dto.ClinicSettingsDto;
import com.clinica.backend.service.ClinicSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/settings/clinic")
@RequiredArgsConstructor
public class ClinicSettingsController {

    private final ClinicSettingsService service;

    @GetMapping
    public ResponseEntity<ClinicSettingsDto> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping
    public ResponseEntity<ClinicSettingsDto> updateSettings(@RequestBody ClinicSettingsDto dto) {
        return ResponseEntity.ok(service.updateSettings(dto));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClinicSettingsDto> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadLogo(file));
    }
    
    @GetMapping("/logo/{filename:.+}")
    public ResponseEntity<Resource> serveLogo(@PathVariable String filename) {
        try {
            Path file = Paths.get("uploads/logos/").resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                String contentType = "image/jpeg";
                if (filename.toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if (filename.toLowerCase().endsWith(".svg")) {
                    contentType = "image/svg+xml";
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
