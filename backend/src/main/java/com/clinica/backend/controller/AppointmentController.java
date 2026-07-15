package com.clinica.backend.controller;
import com.clinica.backend.dto.AppointmentDto;
import com.clinica.backend.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService service;
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentDto>> getByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.getByPatientId(patientId));
    }
    @GetMapping
    public ResponseEntity<List<AppointmentDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
    @PostMapping
    public ResponseEntity<AppointmentDto> create(@RequestBody AppointmentDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentDto> update(@PathVariable Long id, @RequestBody AppointmentDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AppointmentDto> updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> payload) {
        return ResponseEntity.ok(service.updateStatus(id, payload.get("status")));
    }
}
