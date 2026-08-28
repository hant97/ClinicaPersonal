package com.clinica.backend.service;

import com.clinica.backend.dto.WebsiteDraftDto;
import com.clinica.backend.dto.WebsiteEditorDto;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.model.WebsiteLandingDraft;
import com.clinica.backend.model.WebsiteSettings;
import com.clinica.backend.repository.WebsiteLandingDraftRepository;
import com.clinica.backend.repository.WebsiteSettingsRepository;
import jakarta.validation.ConstraintViolationException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cubre la orquestación de la sesión de edición (revisiones, publicación,
 * assets del borrador). La reconciliación en sí — cómo se traduce el
 * borrador a las entidades publicadas — se prueba por separado en
 * {@link WebsiteDraftPublishServiceTest}; aquí solo se verifica que
 * {@code publish()} le delega ese trabajo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebsiteSettingsServiceTest {

    @Mock private WebsiteSettingsRepository settingsRepository;
    @Mock private WebsiteLandingDraftRepository draftRepository;
    @Mock private WebsiteFileStorage fileStorage;
    @Mock private ObjectMapper objectMapper;
    @Mock private WebsiteDraftValidationService draftValidationService;
    @Mock private WebsiteDraftPublishService draftPublishService;

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
        verifyNoInteractions(draftPublishService);
    }

    @Test
    void publishRejectsStaleRevision() {
        assertThrows(ConflictException.class, () -> service.publish(2L));
    }

    @Test
    void publishDelegatesReconciliationAndMarksDraftAsSynced() {
        WebsiteEditorDto editor = service.publish(3L);

        verify(draftPublishService).applyDraftToSettings(dto, settings);
        verify(draftPublishService).reconcileSpecialties(dto);
        verify(draftPublishService).reconcileBenefits(dto);
        verify(draftPublishService).reconcileProcessSteps(dto);
        verify(draftPublishService).reconcileProfessionals(dto);
        verify(draftPublishService).promoteSettingsImages(dto, settings);
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
        ConstraintViolationException exception = mock(ConstraintViolationException.class);
        doThrow(exception).when(draftValidationService).validate(dto);

        assertThrows(ConstraintViolationException.class, () -> service.publish(3L));

        verify(settingsRepository, never()).save(any());
        verifyNoInteractions(draftPublishService);
    }

    @Test
    void publishRejectsInvalidUrlBeforeWriting() {
        dto.setFacebookUrl("no-es-una-url");
        doThrow(new IllegalArgumentException("Una URL del sitio no es válida"))
                .when(draftValidationService).validate(dto);

        assertThrows(IllegalArgumentException.class, () -> service.publish(3L));

        verify(settingsRepository, never()).save(any());
        verifyNoInteractions(draftPublishService);
    }

    @Test
    void resetDraftRebuildsContentFromPublishedVersion() {
        settings.setCommercialName("Nombre público");
        when(draftPublishService.buildDraftFromPublished(settings)).thenReturn(new WebsiteDraftDto());

        service.resetDraft(3L);

        assertEquals(4L, draft.getRevision());
        verify(draftRepository).save(draft);
        verify(draftPublishService).buildDraftFromPublished(settings);
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
