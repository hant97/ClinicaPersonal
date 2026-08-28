package com.clinica.backend.service;

import com.clinica.backend.dto.WebsiteDraftDto;
import com.clinica.backend.dto.WebsiteEditorDto;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.model.User;
import com.clinica.backend.model.WebsiteLandingDraft;
import com.clinica.backend.model.WebsiteSettings;
import com.clinica.backend.repository.WebsiteLandingDraftRepository;
import com.clinica.backend.repository.WebsiteSettingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Gestiona la sesión de edición del landing (el borrador y sus revisiones):
 * obtenerlo, guardarlo, publicarlo, descartarlo y administrar sus assets.
 * <p>
 * La reconciliación de lo publicado (borrador → entidades persistidas) vive
 * en {@link WebsiteDraftPublishService}; el contenido público de solo
 * lectura vive en {@link PublicLandingService}. Esta clase orquesta el
 * flujo de publicación pero no conoce los detalles de cómo se traduce cada
 * sección del borrador a sus entidades.
 */
@Service
@RequiredArgsConstructor
public class WebsiteSettingsService {
    private static final String SINGLETON_KEY = "S";
    private static final Set<String> ALLOWED_IMAGE_CATEGORIES = Set.of("logo", "hero", "approach", "seo");
    private final WebsiteSettingsRepository settingsRepository;
    private final WebsiteLandingDraftRepository draftRepository;
    private final WebsiteFileStorage fileStorage;
    private final ObjectMapper objectMapper;
    private final WebsiteDraftValidationService draftValidationService;
    private final WebsiteDraftPublishService draftPublishService;

    // ===================== Editor visual (borrador) =====================

    @Transactional
    public WebsiteEditorDto getEditor() {
        WebsiteLandingDraft draft = requireDraft();
        WebsiteSettings settings = requireSettings();
        WebsiteDraftDto dto = parseDraft(draft.getContent());
        if (ensureDraftKeys(dto)) {
            draft.setContent(serializeDraft(dto));
            draftRepository.save(draft);
        }
        return toEditor(draft, settings, dto);
    }

    @Transactional
    public WebsiteEditorDto saveDraft(WebsiteDraftDto dto, long revision) {
        WebsiteLandingDraft draft = requireDraft();
        requireRevision(draft, revision);
        ensureDraftKeys(dto);
        normalizeDisplayOrder(dto);
        draft.setContent(serializeDraft(dto));
        draft.setRevision(draft.getRevision() + 1);
        draft.setUpdatedBy(currentUserId());
        draftRepository.save(draft);
        return getEditor();
    }

    @Transactional
    public WebsiteEditorDto publish(long revision) {
        WebsiteLandingDraft draft = requireDraft();
        requireRevision(draft, revision);
        WebsiteDraftDto dto = parseDraft(draft.getContent());
        ensureDraftKeys(dto);
        draftValidationService.validate(dto);
        normalizeDisplayOrder(dto);

        Long userId = currentUserId();
        WebsiteSettings settings = requireSettings();

        draftPublishService.applyDraftToSettings(dto, settings);
        draftPublishService.reconcileSpecialties(dto);
        draftPublishService.reconcileBenefits(dto);
        draftPublishService.reconcileProcessSteps(dto);
        draftPublishService.reconcileProfessionals(dto);
        draftPublishService.promoteSettingsImages(dto, settings);

        settings.setPublishedRevision(draft.getRevision());
        settings.setPublishedAt(LocalDateTime.now());
        settings.setPublishedBy(userId);
        settingsRepository.save(settings);

        draft.setContent(serializeDraft(dto));
        draft.setUpdatedBy(userId);
        draftRepository.save(draft);

        return getEditor();
    }

    @Transactional
    public WebsiteEditorDto resetDraft(long revision) {
        WebsiteLandingDraft draft = requireDraft();
        requireRevision(draft, revision);
        WebsiteDraftDto dto = draftPublishService.buildDraftFromPublished(requireSettings());
        draft.setContent(serializeDraft(dto));
        draft.setRevision(draft.getRevision() + 1);
        draft.setUpdatedBy(currentUserId());
        draftRepository.save(draft);
        return getEditor();
    }

    @Transactional
    public WebsiteEditorDto uploadDraftAsset(String category, MultipartFile file) {
        if (!ALLOWED_IMAGE_CATEGORIES.contains(category)) throw new IllegalArgumentException("Tipo de imagen no permitido");
        WebsiteLandingDraft draft = requireDraft();
        WebsiteDraftDto dto = parseDraft(draft.getContent());
        ensureDraftKeys(dto);
        String previous = getDraftAssetKey(dto, category);
        String key = fileStorage.storeDraft(file, category);
        if (previous != null && fileStorage.isDraftKey(previous)) fileStorage.delete(previous);
        setDraftAssetKey(dto, category, key);
        draft.setContent(serializeDraft(dto));
        draft.setRevision(draft.getRevision() + 1);
        draft.setUpdatedBy(currentUserId());
        draftRepository.save(draft);
        return getEditor();
    }

    @Transactional
    public WebsiteEditorDto deleteDraftAsset(String category) {
        if (!ALLOWED_IMAGE_CATEGORIES.contains(category)) throw new IllegalArgumentException("Tipo de imagen no permitido");
        WebsiteLandingDraft draft = requireDraft();
        WebsiteDraftDto dto = parseDraft(draft.getContent());
        String previous = getDraftAssetKey(dto, category);
        if (previous != null && fileStorage.isDraftKey(previous)) fileStorage.delete(previous);
        setDraftAssetKey(dto, category, null);
        draft.setContent(serializeDraft(dto));
        draft.setRevision(draft.getRevision() + 1);
        draft.setUpdatedBy(currentUserId());
        draftRepository.save(draft);
        return getEditor();
    }

    @Transactional
    public WebsiteEditorDto uploadDraftProfessionalPhoto(String draftKey, MultipartFile file) {
        WebsiteLandingDraft draft = requireDraft();
        WebsiteDraftDto dto = parseDraft(draft.getContent());
        WebsiteDraftDto.Professional professional = dto.getProfessionals().stream()
                .filter(p -> draftKey.equals(p.getDraftKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado en el borrador"));
        String key = fileStorage.storeDraft(file, "professionals");
        if (professional.getPhotoAssetKey() != null && fileStorage.isDraftKey(professional.getPhotoAssetKey())) {
            fileStorage.delete(professional.getPhotoAssetKey());
        }
        professional.setPhotoAssetKey(key);
        draft.setContent(serializeDraft(dto));
        draft.setRevision(draft.getRevision() + 1);
        draft.setUpdatedBy(currentUserId());
        draftRepository.save(draft);
        return getEditor();
    }

    // ===================== Helpers del editor =====================

    private WebsiteLandingDraft requireDraft() {
        return draftRepository.findBySingletonKey(SINGLETON_KEY).orElseThrow();
    }

    private WebsiteSettings requireSettings() {
        return settingsRepository.findBySingletonKey(SINGLETON_KEY).orElseThrow();
    }

    private void requireRevision(WebsiteLandingDraft draft, long revision) {
        if (draft.getRevision() != revision) {
            throw new ConflictException("La revisión del borrador está desactualizada. Recarga el editor e inténtalo de nuevo.");
        }
    }

    private WebsiteDraftDto parseDraft(String content) {
        try {
            return objectMapper.readValue(content, WebsiteDraftDto.class);
        } catch (JacksonException ex) {
            throw new IllegalStateException("El borrador guardado no es válido", ex);
        }
    }

    private String serializeDraft(WebsiteDraftDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JacksonException ex) {
            throw new IllegalStateException("No se pudo guardar el borrador", ex);
        }
    }

    private boolean ensureDraftKeys(WebsiteDraftDto dto) {
        boolean changed = false;
        for (WebsiteDraftDto.Specialty item : dto.getSpecialties()) changed |= assignDraftKey(item::getDraftKey, item::setDraftKey);
        for (WebsiteDraftDto.Benefit item : dto.getBenefits()) changed |= assignDraftKey(item::getDraftKey, item::setDraftKey);
        for (WebsiteDraftDto.ProcessStep item : dto.getProcessSteps()) changed |= assignDraftKey(item::getDraftKey, item::setDraftKey);
        for (WebsiteDraftDto.Professional item : dto.getProfessionals()) changed |= assignDraftKey(item::getDraftKey, item::setDraftKey);
        return changed;
    }

    private boolean assignDraftKey(Supplier<String> getter, Consumer<String> setter) {
        String key = getter.get();
        if (key == null || key.isBlank()) {
            setter.accept(UUID.randomUUID().toString());
            return true;
        }
        return false;
    }

    private void normalizeDisplayOrder(WebsiteDraftDto dto) {
        int order = 1;
        for (WebsiteDraftDto.Specialty item : dto.getSpecialties()) item.setDisplayOrder(order++);
        order = 1;
        for (WebsiteDraftDto.Benefit item : dto.getBenefits()) item.setDisplayOrder(order++);
        order = 1;
        for (WebsiteDraftDto.ProcessStep item : dto.getProcessSteps()) {
            item.setDisplayOrder(order);
            item.setStepNumber(order);
            order++;
        }
        order = 1;
        for (WebsiteDraftDto.Professional item : dto.getProfessionals()) item.setDisplayOrder(order++);
    }

    private WebsiteEditorDto toEditor(WebsiteLandingDraft draft, WebsiteSettings settings, WebsiteDraftDto dto) {
        WebsiteEditorDto editor = new WebsiteEditorDto();
        editor.setDraft(dto);
        editor.setRevision(draft.getRevision());
        editor.setPublishedRevision(settings.getPublishedRevision());
        editor.setHasUnpublishedChanges(draft.getRevision() != settings.getPublishedRevision());
        editor.setDraftUpdatedAt(draft.getUpdatedAt());
        editor.setDraftUpdatedBy(draft.getUpdatedBy());
        editor.setPublishedAt(settings.getPublishedAt());
        editor.setPublishedBy(settings.getPublishedBy());
        return editor;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    private String getDraftAssetKey(WebsiteDraftDto d, String category) {
        return switch (category) {
            case "logo" -> d.getLogoAssetKey();
            case "hero" -> d.getHeroAssetKey();
            case "approach" -> d.getApproachAssetKey();
            case "seo" -> d.getSeoAssetKey();
            default -> throw new IllegalArgumentException("Tipo de imagen no permitido");
        };
    }

    private void setDraftAssetKey(WebsiteDraftDto d, String category, String key) {
        switch (category) {
            case "logo" -> d.setLogoAssetKey(key);
            case "hero" -> d.setHeroAssetKey(key);
            case "approach" -> d.setApproachAssetKey(key);
            case "seo" -> d.setSeoAssetKey(key);
            default -> throw new IllegalArgumentException("Tipo de imagen no permitido");
        }
    }
}
