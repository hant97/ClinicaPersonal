package com.clinica.backend.controller;

import com.clinica.backend.dto.PublicAppointmentConfirmationDto;
import com.clinica.backend.service.AppointmentConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/appointments")
@RequiredArgsConstructor
public class PublicAppointmentController {

    private final AppointmentConfirmationService confirmationService;

    @GetMapping("/confirm/{token}")
    public ResponseEntity<PublicAppointmentConfirmationDto> confirm(@PathVariable String token) {
        return ResponseEntity.ok(confirmationService.confirmByToken(token));
    }
}
