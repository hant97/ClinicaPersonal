package com.clinica.backend.service;

import com.clinica.backend.dto.PsychometricTestDto;
import com.clinica.backend.model.PsychometricTest;
import com.clinica.backend.repository.AssessmentRepository;
import com.clinica.backend.repository.PsychometricTestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PsychometricTestServiceTest {

    private PsychometricTestService buildService(
            PsychometricTestRepository testRepository, AssessmentRepository assessmentRepository) {
        return new PsychometricTestService(testRepository, assessmentRepository);
    }

    @Test
    void getAllTestsShouldEnrichUsageStats() {
        PsychometricTestRepository testRepository = mock(PsychometricTestRepository.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        PsychometricTestService service = buildService(testRepository, assessmentRepository);

        PsychometricTest test = new PsychometricTest();
        test.setId(5L);
        test.setName("BDI-II");
        test.setDescription("Inventario de depresión de Beck");
        test.setQuestionsJson("[{\"id\":1,\"text\":\"Pregunta\",\"options\":[]}]");
        test.setInterpretationJson("[{\"minScore\":0,\"maxScore\":13,\"label\":\"Mínimo\",\"color\":\"emerald\",\"description\":\"Sin síntomas\"}]");

        when(testRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(test), PageRequest.of(0, 10), 1));

        LocalDateTime lastUsed = LocalDateTime.of(2026, 8, 10, 9, 30);
        when(assessmentRepository.usageStatsByTestId())
                .thenReturn(List.<Object[]>of(new Object[]{5L, 3L, lastUsed}));

        Page<PsychometricTestDto> result = service.getAllTests(PageRequest.of(0, 10));

        PsychometricTestDto dto = result.getContent().get(0);
        assertEquals("BDI-II", dto.getName());
        assertEquals(3L, dto.getUsageCount());
        assertEquals(lastUsed, dto.getLastUsedAt());
        assertNotNull(dto.getInterpretationJson());
    }

    @Test
    void getAllTestsShouldLeaveZeroUsageWhenUnused() {
        PsychometricTestRepository testRepository = mock(PsychometricTestRepository.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        PsychometricTestService service = buildService(testRepository, assessmentRepository);

        PsychometricTest test = new PsychometricTest();
        test.setId(9L);
        test.setName("Test sin uso");
        test.setQuestionsJson("[]");

        when(testRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(test), PageRequest.of(0, 10), 1));
        when(assessmentRepository.usageStatsByTestId()).thenReturn(List.of());

        Page<PsychometricTestDto> result = service.getAllTests(PageRequest.of(0, 10));

        PsychometricTestDto dto = result.getContent().get(0);
        assertEquals(0L, dto.getUsageCount());
        assertNull(dto.getLastUsedAt());
    }

    @Test
    void createTestShouldRejectOverlappingBands() {
        PsychometricTestRepository testRepository = mock(PsychometricTestRepository.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        PsychometricTestService service = buildService(testRepository, assessmentRepository);

        PsychometricTestDto dto = new PsychometricTestDto();
        dto.setName("Test");
        dto.setDescription("Descripción");
        dto.setQuestionsJson("[]");
        dto.setInterpretationJson("[{\"minScore\":0,\"maxScore\":10,\"label\":\"A\"},{\"minScore\":10,\"maxScore\":20,\"label\":\"B\"}]");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createTest(dto));
        assertTrue(ex.getMessage().contains("solaparse"));
        verify(testRepository, never()).save(any());
    }

    @Test
    void createTestShouldRejectBandWithMinGreaterThanMax() {
        PsychometricTestRepository testRepository = mock(PsychometricTestRepository.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        PsychometricTestService service = buildService(testRepository, assessmentRepository);

        PsychometricTestDto dto = new PsychometricTestDto();
        dto.setName("Test");
        dto.setDescription("Descripción");
        dto.setQuestionsJson("[]");
        dto.setInterpretationJson("[{\"minScore\":20,\"maxScore\":10,\"label\":\"A\"}]");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createTest(dto));
        assertTrue(ex.getMessage().contains("mínimo menor o igual"));
        verify(testRepository, never()).save(any());
    }

    @Test
    void createTestShouldAcceptValidNonOverlappingBands() {
        PsychometricTestRepository testRepository = mock(PsychometricTestRepository.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        PsychometricTestService service = buildService(testRepository, assessmentRepository);

        when(testRepository.save(any(PsychometricTest.class))).thenAnswer(invocation -> {
            PsychometricTest saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PsychometricTestDto dto = new PsychometricTestDto();
        dto.setName("Test");
        dto.setDescription("Descripción");
        dto.setQuestionsJson("[]");
        dto.setInterpretationJson("[{\"minScore\":0,\"maxScore\":10,\"label\":\"A\"},{\"minScore\":11,\"maxScore\":20,\"label\":\"B\"}]");

        PsychometricTestDto created = service.createTest(dto);

        assertEquals(1L, created.getId());
        assertEquals("Test", created.getName());
    }
}
