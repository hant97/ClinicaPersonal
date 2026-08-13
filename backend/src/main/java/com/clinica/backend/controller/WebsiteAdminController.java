package com.clinica.backend.controller;

import com.clinica.backend.dto.WebsiteSettingsAdminDto;
import com.clinica.backend.service.WebsiteSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/website")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SITE_ADMIN')")
public class WebsiteAdminController {
    private final WebsiteSettingsService service;

    @GetMapping
    public ResponseEntity<WebsiteSettingsAdminDto> get() { return ResponseEntity.ok(service.getAdminSettings()); }

    @PutMapping
    public ResponseEntity<WebsiteSettingsAdminDto> update(@Valid @RequestBody WebsiteSettingsAdminDto dto) { return ResponseEntity.ok(service.update(dto)); }

    @PostMapping(value = "/assets/{category}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebsiteSettingsAdminDto> upload(@PathVariable String category, @RequestParam("file") MultipartFile file) { return ResponseEntity.ok(service.uploadAsset(category, file)); }

    @DeleteMapping("/assets/{category}")
    public ResponseEntity<WebsiteSettingsAdminDto> delete(@PathVariable String category) { return ResponseEntity.ok(service.deleteAsset(category)); }

    @PostMapping(value = "/professionals/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebsiteSettingsAdminDto> uploadProfessionalPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) { return ResponseEntity.ok(service.uploadProfessionalAsset(id, file)); }
}
