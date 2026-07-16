package com.clinica.backend.controller;

import com.clinica.backend.dto.MedicalRecordDto;
import com.clinica.backend.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecordDto>> getRecordsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicalRecordService.getRecordsByPatientId(patientId));
    }

    @PostMapping
    public ResponseEntity<MedicalRecordDto> createRecord(@RequestBody MedicalRecordDto dto) {
        return ResponseEntity.ok(medicalRecordService.createRecord(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecordDto> updateRecord(@PathVariable Long id, @RequestBody MedicalRecordDto dto) {
        return ResponseEntity.ok(medicalRecordService.updateRecord(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        medicalRecordService.deleteRecord(id);
        return ResponseEntity.ok().build();
    }
}
