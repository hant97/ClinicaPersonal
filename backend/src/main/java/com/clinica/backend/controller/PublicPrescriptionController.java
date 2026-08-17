package com.clinica.backend.controller;

import com.clinica.backend.dto.PublicPrescriptionVerificationDto;
import com.clinica.backend.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/prescriptions")
@RequiredArgsConstructor
public class PublicPrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/verify/{code}")
    public ResponseEntity<PublicPrescriptionVerificationDto> verifyPrescription(@PathVariable String code) {
        return ResponseEntity.ok(prescriptionService.verifyPrescription(code));
    }
}
