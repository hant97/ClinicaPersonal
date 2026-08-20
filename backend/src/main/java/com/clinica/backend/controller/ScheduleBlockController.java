package com.clinica.backend.controller;

import com.clinica.backend.dto.ScheduleBlockDto;
import com.clinica.backend.service.ScheduleBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedule-blocks")
@RequiredArgsConstructor
public class ScheduleBlockController {

    private final ScheduleBlockService blockService;

    @GetMapping
    public ResponseEntity<List<ScheduleBlockDto>> getBlocks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long professionalId) {
        return ResponseEntity.ok(blockService.getBlocks(startDate, endDate, professionalId));
    }

    @PostMapping
    public ResponseEntity<ScheduleBlockDto> createBlock(@Valid @RequestBody ScheduleBlockDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blockService.createBlock(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleBlockDto> updateBlock(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleBlockDto dto) {
        return ResponseEntity.ok(blockService.updateBlock(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlock(@PathVariable Long id) {
        blockService.deleteBlock(id);
        return ResponseEntity.noContent().build();
    }
}
