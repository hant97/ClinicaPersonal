package com.clinica.backend.controller;

import com.clinica.backend.dto.PublicLandingDto;
import com.clinica.backend.service.PublicLandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicLandingController {
    private final PublicLandingService service;

    @GetMapping("/landing")
    public ResponseEntity<PublicLandingDto> getLanding() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getPublicLanding());
    }

    @GetMapping("/website-assets/{*key}")
    public ResponseEntity<Resource> getAsset(@PathVariable String key) {
        Resource resource = service.loadAsset(key.startsWith("/") ? key.substring(1) : key);
        String filename = resource.getFilename() == null ? "" : resource.getFilename().toLowerCase();
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG
                : filename.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'")
                .contentType(type)
                .body(resource);
    }
}
