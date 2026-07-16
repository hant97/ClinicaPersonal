package com.clinica.backend.controller;

import com.clinica.backend.dto.PsychometricTestDto;
import com.clinica.backend.service.PsychometricTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/tests")
@CrossOrigin(origins = "*")
public class PsychometricTestController {

    @Autowired
    private PsychometricTestService psychometricTestService;

    @GetMapping
    public ResponseEntity<Page<PsychometricTestDto>> getAllTests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(psychometricTestService.getAllTests(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PsychometricTestDto> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(psychometricTestService.getTestById(id));
    }

    @PostMapping
    public ResponseEntity<PsychometricTestDto> createTest(@RequestBody PsychometricTestDto dto) {
        return ResponseEntity.ok(psychometricTestService.createTest(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PsychometricTestDto> updateTest(@PathVariable Long id, @RequestBody PsychometricTestDto dto) {
        return ResponseEntity.ok(psychometricTestService.updateTest(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long id) {
        psychometricTestService.deleteTest(id);
        return ResponseEntity.noContent().build();
    }
}
