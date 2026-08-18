package com.clinica.backend.controller;

import com.clinica.backend.dto.PatientDto;
import com.clinica.backend.dto.PatientStatsDto;
import com.clinica.backend.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    public ResponseEntity<Page<PatientDto>> getAllPatients(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String gender,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(patientService.getAllPatients(active, gender, PageRequest.of(page, size)));
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(patientService.searchPatients(query, active, gender, PageRequest.of(page, size)));
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
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PatientDto> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(patientService.uploadPhoto(id, file));
    }
}
