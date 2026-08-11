package com.clinica.backend.service;

import com.clinica.backend.dto.PsychometricTestDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.PsychometricTest;
import com.clinica.backend.repository.AssessmentRepository;
import com.clinica.backend.repository.PsychometricTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PsychometricTestService {

    private final PsychometricTestRepository psychometricTestRepository;
    private final AssessmentRepository assessmentRepository;

    @Transactional(readOnly = true)
    public Page<PsychometricTestDto> getAllTests(Pageable pageable) {
        return psychometricTestRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public PsychometricTestDto getTestById(Long id) {
        PsychometricTest test = psychometricTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test psicométrico", "id", id));
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
                .orElseThrow(() -> new ResourceNotFoundException("Test psicométrico", "id", id));
                
        test.setName(dto.getName());
        test.setDescription(dto.getDescription());
        test.setQuestionsJson(dto.getQuestionsJson());
        
        PsychometricTest updated = psychometricTestRepository.save(test);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteTest(Long id) {
        if (assessmentRepository.existsByPsychometricTestId(id)) {
            throw new IllegalArgumentException("No se puede eliminar el test porque ya tiene evaluaciones registradas por pacientes.");
        }
        
        PsychometricTest test = psychometricTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test psicométrico", "id", id));
        psychometricTestRepository.delete(test);
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
