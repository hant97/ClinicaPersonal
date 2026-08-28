package com.clinica.backend.service;

import com.clinica.backend.dto.WebsiteDraftDto;
import com.clinica.backend.model.WebsiteBenefit;
import com.clinica.backend.repository.WebsiteBenefitRepository;
import com.clinica.backend.repository.WebsiteProcessStepRepository;
import com.clinica.backend.repository.WebsiteProfessionalRepository;
import com.clinica.backend.repository.WebsiteSpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba la reconciliación borrador → entidades publicadas de forma
 * aislada de la sesión de edición (ver {@link WebsiteSettingsServiceTest}
 * para la orquestación de publish()/resetDraft()).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebsiteDraftPublishServiceTest {

    @Mock private WebsiteSpecialtyRepository specialtyRepository;
    @Mock private WebsiteBenefitRepository benefitRepository;
    @Mock private WebsiteProcessStepRepository processStepRepository;
    @Mock private WebsiteProfessionalRepository professionalRepository;
    @Mock private WebsiteFileStorage fileStorage;

    @InjectMocks private WebsiteDraftPublishService draftPublishService;

    private WebsiteDraftDto dto;

    @BeforeEach
    void setUp() {
        dto = new WebsiteDraftDto();
        when(specialtyRepository.findAll()).thenReturn(Collections.emptyList());
        when(benefitRepository.findAll()).thenReturn(Collections.emptyList());
        when(processStepRepository.findAll()).thenReturn(Collections.emptyList());
        when(professionalRepository.findAll()).thenReturn(Collections.emptyList());
    }

    @Test
    void reconcileBenefitsUpdatesExistingAndDeletesRemovedItems() {
        WebsiteBenefit existing = new WebsiteBenefit();
        existing.setId(1L);
        existing.setTitle("Antiguo");
        WebsiteBenefit removed = new WebsiteBenefit();
        removed.setId(99L);
        removed.setTitle("Eliminado");
        when(benefitRepository.findAll()).thenReturn(List.of(existing, removed));

        WebsiteDraftDto.Benefit benefit = new WebsiteDraftDto.Benefit();
        benefit.setId(1L);
        benefit.setTitle("Actualizado");
        benefit.setIconCode("SPARKLES");
        dto.setBenefits(new ArrayList<>(List.of(benefit)));

        draftPublishService.reconcileBenefits(dto);

        assertEquals("Actualizado", existing.getTitle());
        verify(benefitRepository).save(existing);
        verify(benefitRepository).delete(removed);
    }
}
