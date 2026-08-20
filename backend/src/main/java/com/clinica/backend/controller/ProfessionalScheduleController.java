package com.clinica.backend.controller;

import com.clinica.backend.dto.WeeklyScheduleDto;
import com.clinica.backend.model.User;
import com.clinica.backend.service.ProfessionalScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ProfessionalScheduleController {

    private final ProfessionalScheduleService scheduleService;

    @GetMapping("/professional/{professionalId}")
    public ResponseEntity<WeeklyScheduleDto> getWeeklySchedule(@PathVariable Long professionalId) {
        return ResponseEntity.ok(scheduleService.getWeeklySchedule(professionalId));
    }

    @PutMapping("/professional/{professionalId}")
    public ResponseEntity<WeeklyScheduleDto> saveWeeklySchedule(
            @PathVariable Long professionalId,
            @Valid @RequestBody WeeklyScheduleDto dto) {
        dto.setProfessionalId(professionalId);
        return ResponseEntity.ok(scheduleService.saveWeeklySchedule(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<WeeklyScheduleDto> getMyWeeklySchedule(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(scheduleService.getWeeklySchedule(user.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<WeeklyScheduleDto> saveMyWeeklySchedule(
            Authentication authentication,
            @Valid @RequestBody WeeklyScheduleDto dto) {
        User user = (User) authentication.getPrincipal();
        dto.setProfessionalId(user.getId());
        return ResponseEntity.ok(scheduleService.saveWeeklySchedule(dto));
    }
}
