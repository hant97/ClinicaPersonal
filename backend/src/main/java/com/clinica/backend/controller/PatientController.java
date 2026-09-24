package com.clinica.backend.controller;

import com.clinica.backend.dto.PatientDto;
import com.clinica.backend.dto.PatientStatsDto;
import com.clinica.backend.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    public ResponseEntity<Page<PatientDto>> getAllPatients(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String gender,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(patientService.getAllPatients(active, gender, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<PatientStatsDto> getStats() {
        return ResponseEntity.ok(patientService.getStats());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PatientDto>> searchPatients(
            @RequestParam String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String gender,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(patientService.searchPatients(query, active, gender, pageable));
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<PatientDto> getPatientById(@PathVariable String identifier) {
        return ResponseEntity.ok(patientService.getPatientByIdOrUuid(identifier));
    }

    @PostMapping
    public ResponseEntity<PatientDto> createPatient(@Valid @RequestBody PatientDto patientDto) {
        return ResponseEntity.ok(patientService.createPatient(patientDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientDto> updatePatient(@PathVariable Long id, @Valid @RequestBody PatientDto patientDto) {
        return ResponseEntity.ok(patientService.updatePatient(id, patientDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable Long id) {
        Resource photo = patientService.loadPhoto(id);
        String filename = photo.getFilename() == null ? "" : photo.getFilename().toLowerCase();
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG
                : filename.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                // privada: la URL cambia con cada foto (?v=), así que puede guardarse en caché.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(type)
                .body(photo);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PatientDto> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(patientService.uploadPhoto(id, file));
    }
}
