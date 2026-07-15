package com.clinica.backend.controller;

import com.clinica.backend.dto.MedicalRecordDto;
import com.clinica.backend.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecordDto>> getRecordsByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicalRecordService.getRecordsByPatientId(patientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordDto> getRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(medicalRecordService.getRecordById(id));
    }

    @PostMapping
    public ResponseEntity<MedicalRecordDto> createRecord(@RequestBody MedicalRecordDto medicalRecordDto) {
        return ResponseEntity.ok(medicalRecordService.createRecord(medicalRecordDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecordDto> updateRecord(@PathVariable Long id, @RequestBody MedicalRecordDto medicalRecordDto) {
        return ResponseEntity.ok(medicalRecordService.updateRecord(id, medicalRecordDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        medicalRecordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
