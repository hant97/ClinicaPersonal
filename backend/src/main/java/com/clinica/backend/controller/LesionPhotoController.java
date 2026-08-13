package com.clinica.backend.controller;

import com.clinica.backend.dto.LesionPhotoDto;
import com.clinica.backend.service.LesionPhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LesionPhotoController {

    private final LesionPhotoService service;

    @GetMapping("/lesions/{lesionId}/photos")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<List<LesionPhotoDto>> getPhotos(@PathVariable Long lesionId) {
        return ResponseEntity.ok(service.getPhotos(lesionId));
    }

    @PostMapping("/lesions/{lesionId}/photos")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<LesionPhotoDto> uploadPhoto(
            @PathVariable Long lesionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "takenDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate takenDate) {
        return ResponseEntity.ok(service.uploadPhoto(lesionId, file, description, takenDate));
    }

    @DeleteMapping("/lesion-photos/{id}")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
        service.deletePhoto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lesion-photos/{id}/file")
    @PreAuthorize("principal.specialty == 'DERMATOLOGIA'")
    public ResponseEntity<Resource> getPhotoFile(@PathVariable Long id) {
        Resource resource = service.loadPhoto(id);
        String filename = resource.getFilename() == null ? "" : resource.getFilename().toLowerCase();
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG
                : filename.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(resource);
    }
}
