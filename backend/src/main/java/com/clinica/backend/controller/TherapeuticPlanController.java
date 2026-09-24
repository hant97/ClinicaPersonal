package com.clinica.backend.controller;

import com.clinica.backend.dto.TherapeuticPlanDto;
import com.clinica.backend.service.TherapeuticPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TherapeuticPlanController {

    private final TherapeuticPlanService service;

    @GetMapping("/patients/{patientId}/therapeutic-plans")
    public ResponseEntity<Page<TherapeuticPlanDto>> getPlans(
            @PathVariable Long patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getPlans(patientId, pageable));
    }

    @PostMapping("/patients/{patientId}/therapeutic-plans")
    public ResponseEntity<TherapeuticPlanDto> createPlan(
            @PathVariable Long patientId,
            @Valid @RequestBody TherapeuticPlanDto dto) {
        dto.setPatientId(patientId);
        return ResponseEntity.ok(service.createPlan(patientId, dto));
    }

    @PutMapping("/therapeutic-plans/{id}")
    public ResponseEntity<TherapeuticPlanDto> updatePlan(@PathVariable Long id, @Valid @RequestBody TherapeuticPlanDto dto) {
        return ResponseEntity.ok(service.updatePlan(id, dto));
    }

    @DeleteMapping("/therapeutic-plans/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        service.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
