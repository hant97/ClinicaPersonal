package com.clinica.backend.service;

import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.model.WebsiteSpecialty;
import com.clinica.backend.model.WebsiteSettings;
import com.clinica.backend.repository.WebsiteBenefitRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.WebsiteProcessStepRepository;
import com.clinica.backend.repository.WebsiteProfessionalRepository;
import com.clinica.backend.repository.WebsiteSettingsRepository;
import com.clinica.backend.repository.WebsiteSpecialtyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.clinica.backend.exception.ResourceNotFoundException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublicLandingServiceTest {

    @Mock private WebsiteSettingsRepository settingsRepository;
    @Mock private ClinicalServiceRepository clinicalServiceRepository;
    @Mock private WebsiteSpecialtyRepository specialtyRepository;
    @Mock private WebsiteBenefitRepository benefitRepository;
    @Mock private WebsiteProcessStepRepository processStepRepository;
    @Mock private WebsiteProfessionalRepository professionalRepository;
    @Mock private WebsiteFileStorage fileStorage;

    @InjectMocks private PublicLandingService service;

    @Test
    void loadAssetDelegatesToFileStorage() {
        Resource mockResource = mock(Resource.class);
        when(fileStorage.load("logo/test.png")).thenReturn(mockResource);

        Resource result = service.loadAsset("logo/test.png");
        assertEquals(mockResource, result);
        verify(fileStorage).load("logo/test.png");
    }

    @Test
    void loadAssetNeverServesLegacyPatientPhotos() {
        assertThrows(ResourceNotFoundException.class, () -> service.loadAsset("patients/legacy.jpg"));
        verifyNoInteractions(fileStorage);
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
