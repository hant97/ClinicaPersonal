package com.clinica.backend.service;

import com.clinica.backend.dto.WebsiteDraftDto;
import com.clinica.backend.dto.WebsiteEditorDto;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.model.WebsiteBenefit;
import com.clinica.backend.model.WebsiteLandingDraft;
import com.clinica.backend.model.WebsiteSettings;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.WebsiteBenefitRepository;
import com.clinica.backend.repository.WebsiteLandingDraftRepository;
import com.clinica.backend.repository.WebsiteProcessStepRepository;
import com.clinica.backend.repository.WebsiteProfessionalRepository;
import com.clinica.backend.repository.WebsiteSettingsRepository;
import com.clinica.backend.repository.WebsiteSpecialtyRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebsiteEditorServiceTest {

    @Mock private WebsiteSettingsRepository settingsRepository;
    @Mock private WebsiteLandingDraftRepository draftRepository;
    @Mock private ClinicalServiceRepository clinicalServiceRepository;
    @Mock private WebsiteSpecialtyRepository specialtyRepository;
    @Mock private WebsiteBenefitRepository benefitRepository;
    @Mock private WebsiteProcessStepRepository processStepRepository;
    @Mock private WebsiteProfessionalRepository professionalRepository;
    @Mock private WebsiteFileStorage fileStorage;
    @Mock private ObjectMapper objectMapper;
    @Mock private Validator validator;

    @InjectMocks private WebsiteSettingsService service;

    private WebsiteLandingDraft draft;
    private WebsiteSettings settings;
    private WebsiteDraftDto dto;

    @BeforeEach
    void setUp() throws Exception {
        draft = new WebsiteLandingDraft();
        draft.setRevision(3L);
        draft.setContent("{}");

        settings = new WebsiteSettings();

        dto = new WebsiteDraftDto();
        dto.setCommercialName("Clínica Demo");
        dto.setHeroTitle("Atención integral");

        when(draftRepository.findBySingletonKey("S")).thenReturn(Optional.of(draft));
        when(settingsRepository.findBySingletonKey("S")).thenReturn(Optional.of(settings));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(objectMapper.readValue(anyString(), eq(WebsiteDraftDto.class))).thenReturn(dto);
        when(validator.validate(any(WebsiteDraftDto.class))).thenReturn(Collections.emptySet());
        when(specialtyRepository.findAll()).thenReturn(Collections.emptyList());
        when(benefitRepository.findAll()).thenReturn(Collections.emptyList());
        when(processStepRepository.findAll()).thenReturn(Collections.emptyList());
        when(professionalRepository.findAll()).thenReturn(Collections.emptyList());
    }

    @Test
    void saveDraftRejectsStaleRevision() {
        assertThrows(ConflictException.class, () -> service.saveDraft(dto, 2L));
    }

    @Test
    void saveDraftIncrementsRevisionWithoutTouchingPublicContent() {
        service.saveDraft(dto, 3L);

        assertEquals(4L, draft.getRevision());
        verify(settingsRepository, never()).save(any());
        verify(specialtyRepository, never()).save(any());
        verify(benefitRepository, never()).save(any());
        verify(processStepRepository, never()).save(any());
        verify(professionalRepository, never()).save(any());
    }

    @Test
    void publishRejectsStaleRevision() {
        assertThrows(ConflictException.class, () -> service.publish(2L));
    }

    @Test
    void publishPersistsContentAndMarksDraftAsSynced() {
        WebsiteEditorDto editor = service.publish(3L);

        assertEquals(3L, settings.getPublishedRevision());
        verify(settingsRepository).save(settings);
        assertFalse(editor.isHasUnpublishedChanges());
    }

    @Test
    void getEditorReportsHasUnpublishedChanges() {
        settings.setPublishedRevision(1L);
        WebsiteEditorDto editor = service.getEditor();

        assertEquals(3L, editor.getRevision());
        assertTrue(editor.isHasUnpublishedChanges());
    }

    @Test
    void publishDoesNotWriteAnythingWhenDraftIsInvalid() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<WebsiteDraftDto> violation = mock(ConstraintViolation.class);
        when(validator.validate(any(WebsiteDraftDto.class))).thenReturn(Collections.singleton(violation));

        assertThrows(ConstraintViolationException.class, () -> service.publish(3L));

        verify(settingsRepository, never()).save(any());
        verify(specialtyRepository, never()).save(any());
        verify(benefitRepository, never()).save(any());
        verify(benefitRepository, never()).delete(any());
        verify(processStepRepository, never()).save(any());
        verify(professionalRepository, never()).save(any());
    }

    @Test
    void publishRejectsInvalidUrlBeforeWriting() {
        dto.setFacebookUrl("no-es-una-url");

        assertThrows(IllegalArgumentException.class, () -> service.publish(3L));

        verify(settingsRepository, never()).save(any());
        verify(benefitRepository, never()).save(any());
    }

    @Test
    void publishReconcilesCollectionsAndDeletesRemovedItems() {
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

        service.publish(3L);

        assertEquals("Actualizado", existing.getTitle());
        verify(benefitRepository).save(existing);
        verify(benefitRepository).delete(removed);
    }

    @Test
    void resetDraftRebuildsContentFromPublishedVersion() {
        settings.setCommercialName("Nombre público");

        service.resetDraft(3L);

        assertEquals(4L, draft.getRevision());
        verify(draftRepository).save(draft);
    }

    @Test
    void uploadDraftAssetDoesNotDeletePublishedImage() {
        dto.setHeroAssetKey("hero/published.png");
        when(fileStorage.storeDraft(any(MultipartFile.class), eq("hero"))).thenReturn("draft/hero/new.png");

        service.uploadDraftAsset("hero", mock(MultipartFile.class));

        verify(fileStorage, never()).delete("hero/published.png");
        assertEquals("draft/hero/new.png", dto.getHeroAssetKey());
    }

    @Test
    void uploadDraftAssetDeletesPreviousDraftImage() {
        dto.setHeroAssetKey("draft/hero/old.png");
        when(fileStorage.isDraftKey("draft/hero/old.png")).thenReturn(true);
        when(fileStorage.storeDraft(any(MultipartFile.class), eq("hero"))).thenReturn("draft/hero/new.png");

        service.uploadDraftAsset("hero", mock(MultipartFile.class));

        verify(fileStorage).delete("draft/hero/old.png");
        assertEquals("draft/hero/new.png", dto.getHeroAssetKey());
    }
}
