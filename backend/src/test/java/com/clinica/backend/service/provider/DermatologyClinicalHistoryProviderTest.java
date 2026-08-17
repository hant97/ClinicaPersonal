package com.clinica.backend.service.provider;

import com.clinica.backend.dto.*;
import com.clinica.backend.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DermatologyClinicalHistoryProviderTest {

    @Mock private DermatologicalHistoryService dermatologicalHistoryService;
    @Mock private LesionService lesionService;
    @Mock private AuxiliaryExamService auxiliaryExamService;
    @Mock private TreatmentService treatmentService;
    @Mock private ProcedureService procedureService;
    @Mock private EvolutionService evolutionService;

    @InjectMocks
    private DermatologyClinicalHistoryProvider provider;

    @Test
    @DisplayName("getSupportedSpecialty should return DERMATOLOGIA")
    void getSupportedSpecialty_ShouldReturnDermatology() {
        assertEquals("DERMATOLOGIA", provider.getSupportedSpecialty());
    }

    @Test
    @DisplayName("populateSections should load all dermatology sections")
    void populateSections_ShouldPopulateDto() {
        ClinicalHistoryDto dto = new ClinicalHistoryDto();
        dto.setPatientId(8L);
        dto.setSpecialty("DERMATOLOGIA");
        Pageable pageable = PageRequest.of(0, 50);

        DermatologicalHistoryDto historyDto = new DermatologicalHistoryDto();
        historyDto.setPatientId(8L);

        when(dermatologicalHistoryService.getHistory(8L)).thenReturn(historyDto);
        when(lesionService.getLesions(eq(8L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new LesionDto())));
        when(auxiliaryExamService.getExams(eq(8L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new AuxiliaryExamDto())));
        when(treatmentService.getTreatments(eq(8L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new TreatmentDto())));
        when(procedureService.getProcedures(eq(8L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new ProcedureDto())));
        when(evolutionService.getEvolutions(eq(8L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new EvolutionDto())));

        provider.populateSections(8L, dto, pageable);

        assertNotNull(dto.getDermatologicalHistory());
        assertNotNull(dto.getLesions());
        assertEquals(1, dto.getLesions().size());
        assertNotNull(dto.getAuxiliaryExams());
        assertEquals(1, dto.getAuxiliaryExams().size());
        assertNotNull(dto.getTreatments());
        assertEquals(1, dto.getTreatments().size());
        assertNotNull(dto.getProcedures());
        assertEquals(1, dto.getProcedures().size());
        assertNotNull(dto.getEvolutions());
        assertEquals(1, dto.getEvolutions().size());

        verify(dermatologicalHistoryService).getHistory(8L);
        verify(lesionService).getLesions(eq(8L), any(Pageable.class));
        verify(auxiliaryExamService).getExams(eq(8L), any(Pageable.class));
        verify(treatmentService).getTreatments(eq(8L), any(Pageable.class));
        verify(procedureService).getProcedures(eq(8L), any(Pageable.class));
        verify(evolutionService).getEvolutions(eq(8L), any(Pageable.class));
    }
}
