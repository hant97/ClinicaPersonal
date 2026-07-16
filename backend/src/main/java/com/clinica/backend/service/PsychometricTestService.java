package com.clinica.backend.service;

import com.clinica.backend.dto.PsychometricTestDto;
import com.clinica.backend.model.PsychometricTest;
import com.clinica.backend.repository.AssessmentRepository;
import com.clinica.backend.repository.PsychometricTestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class PsychometricTestService {

    @Autowired
    private PsychometricTestRepository psychometricTestRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    public Page<PsychometricTestDto> getAllTests(Pageable pageable) {
        return psychometricTestRepository.findAll(pageable).map(this::mapToDto);
    }

    public PsychometricTestDto getTestById(Long id) {
        PsychometricTest test = psychometricTestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test no encontrado"));
        return mapToDto(test);
    }

    @Transactional
    public PsychometricTestDto createTest(PsychometricTestDto dto) {
        PsychometricTest test = new PsychometricTest();
        test.setName(dto.getName());
        test.setDescription(dto.getDescription());
        test.setQuestionsJson(dto.getQuestionsJson());
        
        PsychometricTest saved = psychometricTestRepository.save(test);
        return mapToDto(saved);
    }

    @Transactional
    public PsychometricTestDto updateTest(Long id, PsychometricTestDto dto) {
        PsychometricTest test = psychometricTestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test no encontrado"));
                
        test.setName(dto.getName());
        test.setDescription(dto.getDescription());
        test.setQuestionsJson(dto.getQuestionsJson());
        
        PsychometricTest updated = psychometricTestRepository.save(test);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteTest(Long id) {
        // Verificar si existen evaluaciones asociadas
        long count = assessmentRepository.findAll().stream()
                .filter(a -> a.getPsychometricTest().getId().equals(id))
                .count();
                
        if (count > 0) {
            throw new RuntimeException("No se puede eliminar el test porque ya tiene evaluaciones registradas por pacientes.");
        }
        
        psychometricTestRepository.deleteById(id);
    }

    private PsychometricTestDto mapToDto(PsychometricTest test) {
        PsychometricTestDto dto = new PsychometricTestDto();
        dto.setId(test.getId());
        dto.setName(test.getName());
        dto.setDescription(test.getDescription());
        dto.setQuestionsJson(test.getQuestionsJson());
        return dto;
    }
}
