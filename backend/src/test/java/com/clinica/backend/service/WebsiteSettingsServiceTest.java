package com.clinica.backend.service;

import com.clinica.backend.dto.WebsiteSettingsAdminDto;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.model.WebsiteSpecialty;
import com.clinica.backend.model.WebsiteSettings;
import com.clinica.backend.repository.WebsiteBenefitRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.WebsiteProcessStepRepository;
import com.clinica.backend.repository.WebsiteProfessionalRepository;
import com.clinica.backend.repository.WebsiteSettingsRepository;
import com.clinica.backend.repository.WebsiteSpecialtyRepository;
import com.clinica.backend.repository.WebsiteSpecialtyServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebsiteSettingsServiceTest {

    @Mock private WebsiteSettingsRepository settingsRepository;
    @Mock private ClinicalServiceRepository clinicalServiceRepository;
    @Mock private WebsiteSpecialtyRepository specialtyRepository;
    @Mock private WebsiteSpecialtyServiceRepository specialtyServiceRepository;
    @Mock private WebsiteBenefitRepository benefitRepository;
    @Mock private WebsiteProcessStepRepository processStepRepository;
    @Mock private WebsiteProfessionalRepository professionalRepository;
    @Mock private WebsiteFileStorage fileStorage;

    @InjectMocks private WebsiteSettingsService service;

    @Test
    void updateAllowsOptionalUrlsToBeNull() {
        WebsiteSettings settings = new WebsiteSettings();
        settings.setId(1L);
        WebsiteSettingsAdminDto dto = new WebsiteSettingsAdminDto();
        dto.setCommercialName("Clínica Demo");
        dto.setHeroTitle("Atención integral");
        dto.setContactPhone("999 888 777");
        dto.setContactWhatsapp("999888777");

        when(settingsRepository.findBySingletonKey("S")).thenReturn(Optional.of(settings));
        when(settingsRepository.save(any(WebsiteSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.update(dto));
        verify(specialtyServiceRepository).deleteAllInBatch();
        verify(specialtyRepository).deleteAllInBatch();
        verify(benefitRepository).deleteAllInBatch();
        verify(processStepRepository).deleteAllInBatch();
        verify(professionalRepository).deleteAllInBatch();
    }

    @Test
    void publicLandingUsesActiveClinicalServicesForEachSpecialty() {
        WebsiteSettings settings = new WebsiteSettings();
        WebsiteSpecialty dermatology = new WebsiteSpecialty();
        dermatology.setCode("DERMATOLOGIA");
        dermatology.setVisible(true);
        ClinicalService clinicalService = new ClinicalService();
        clinicalService.setSpecialty("DERMATOLOGIA");
        clinicalService.setName("Consulta dermatológica");

        when(settingsRepository.findBySingletonKey("S")).thenReturn(Optional.of(settings));
        when(specialtyRepository.findAllByOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(dermatology));
        when(clinicalServiceRepository.findAllByDeletedFalseOrderBySpecialtyAscNameAsc()).thenReturn(List.of(clinicalService));

        assertEquals(List.of("Consulta dermatológica"), service.getPublicLanding().getSpecialties().get(0).getServices());
    }
}
