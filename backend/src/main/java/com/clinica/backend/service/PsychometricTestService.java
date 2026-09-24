package com.clinica.backend.service;

import com.clinica.backend.mapper.PsychometricTestMapper;

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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PsychometricTestService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PsychometricTestRepository psychometricTestRepository;
    private final AssessmentRepository assessmentRepository;
    private final PsychometricTestMapper psychometricTestMapper;

    @Transactional(readOnly = true)
    public Page<PsychometricTestDto> getAllTests(Pageable pageable) {
        Map<Long, Object[]> usage = usageStatsByTestId();
        return psychometricTestRepository.findAll(pageable)
                .map(test -> {
                    PsychometricTestDto dto = psychometricTestMapper.toDto(test);
                    Object[] stats = usage.get(test.getId());
                    if (stats != null) {
                        dto.setUsageCount(((Number) stats[1]).longValue());
                        dto.setLastUsedAt((LocalDateTime) stats[2]);
                    }
                    return dto;
                });
    }

    @Transactional(readOnly = true)
    public PsychometricTestDto getTestById(Long id) {
        PsychometricTest test = psychometricTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test psicométrico", "id", id));
        return psychometricTestMapper.toDto(test);
    }

    @Transactional
    public PsychometricTestDto createTest(PsychometricTestDto dto) {
        validateInterpretation(dto.getInterpretationJson());
        PsychometricTest test = new PsychometricTest();
        test.setName(dto.getName());
        test.setDescription(dto.getDescription());
        test.setQuestionsJson(dto.getQuestionsJson());
        test.setInterpretationJson(dto.getInterpretationJson());
        
        PsychometricTest saved = psychometricTestRepository.save(test);
        return psychometricTestMapper.toDto(saved);
    }

    @Transactional
    public PsychometricTestDto updateTest(Long id, PsychometricTestDto dto) {
        validateInterpretation(dto.getInterpretationJson());
        PsychometricTest test = psychometricTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test psicométrico", "id", id));
                
        test.setName(dto.getName());
        test.setDescription(dto.getDescription());
        test.setQuestionsJson(dto.getQuestionsJson());
        test.setInterpretationJson(dto.getInterpretationJson());
        
        PsychometricTest updated = psychometricTestRepository.save(test);
        return psychometricTestMapper.toDto(updated);
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

    private Map<Long, Object[]> usageStatsByTestId() {
        return assessmentRepository.usageStatsByTestId().stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> row));
    }

    private void validateInterpretation(String interpretationJson) {
        if (interpretationJson == null || interpretationJson.isBlank()) {
            return;
        }
        JsonNode array;
        try {
            array = OBJECT_MAPPER.readTree(interpretationJson);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Las bandas de interpretación tienen un formato inválido.");
        }
        if (!array.isArray()) {
            throw new IllegalArgumentException("Las bandas de interpretación deben ser una lista.");
        }

        List<Band> bands = new ArrayList<>();
        for (JsonNode node : array) {
            int min = node.path("minScore").asInt();
            int max = node.path("maxScore").asInt();
            String label = node.path("label").asString("");
            bands.add(new Band(min, max, label));
        }

        for (Band band : bands) {
            if (band.min() > band.max()) {
                throw new IllegalArgumentException("Cada banda debe tener un mínimo menor o igual a su máximo.");
            }
        }

        bands.sort(Comparator.comparingInt(Band::min));
        for (int i = 1; i < bands.size(); i++) {
            Band prev = bands.get(i - 1);
            Band current = bands.get(i);
            if (current.min() <= prev.max()) {
                throw new IllegalArgumentException(
                        "Las bandas de interpretación no deben solaparse. Revisa los rangos \""
                                + prev.label() + "\" y \"" + current.label() + "\".");
            }
        }
    }

    private record Band(int min, int max, String label) {}

}
