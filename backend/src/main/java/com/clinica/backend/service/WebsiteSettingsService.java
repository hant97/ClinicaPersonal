package com.clinica.backend.service;

import com.clinica.backend.dto.PublicLandingDto;
import com.clinica.backend.dto.WebsiteDraftDto;
import com.clinica.backend.dto.WebsiteEditorDto;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.model.WebsiteBenefit;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.model.User;
import com.clinica.backend.model.WebsiteLandingDraft;
import com.clinica.backend.model.WebsiteProcessStep;
import com.clinica.backend.model.WebsiteProfessional;
import com.clinica.backend.model.WebsiteSettings;
import com.clinica.backend.model.WebsiteSpecialty;
import com.clinica.backend.repository.WebsiteBenefitRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.WebsiteLandingDraftRepository;
import com.clinica.backend.repository.WebsiteProcessStepRepository;
import com.clinica.backend.repository.WebsiteProfessionalRepository;
import com.clinica.backend.repository.WebsiteSettingsRepository;
import com.clinica.backend.repository.WebsiteSpecialtyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebsiteSettingsService {
    private static final String SINGLETON_KEY = "S";
    private static final Set<String> ALLOWED_IMAGE_CATEGORIES = Set.of("logo", "hero", "approach", "seo");
    private final WebsiteSettingsRepository settingsRepository;
    private final WebsiteLandingDraftRepository draftRepository;
    private final ClinicalServiceRepository clinicalServiceRepository;
    private final WebsiteSpecialtyRepository specialtyRepository;
    private final WebsiteBenefitRepository benefitRepository;
    private final WebsiteProcessStepRepository processStepRepository;
    private final WebsiteProfessionalRepository professionalRepository;
    private final WebsiteFileStorage fileStorage;
    private final ObjectMapper objectMapper;
    private final WebsiteDraftValidationService draftValidationService;

    @Transactional
    public PublicLandingDto getPublicLanding() {
        WebsiteSettings settings = settingsRepository.findBySingletonKey(SINGLETON_KEY).orElseThrow();
        PublicLandingDto dto = new PublicLandingDto();
        PublicLandingDto.General general = dto.getGeneral(); general.setCommercialName(settings.getCommercialName()); general.setTagline(settings.getTagline()); general.setDescription(settings.getDescription()); general.setLogoUrl(resolve(settings.getLogoAssetKey(), settings.getLogoExternalImageUrl()));
        PublicLandingDto.Hero hero = dto.getHero(); hero.setEyebrow(settings.getHeroEyebrow()); hero.setTitle(settings.getHeroTitle()); hero.setHighlight(settings.getHeroHighlight()); hero.setDescription(settings.getHeroDescription()); hero.setPrimaryButtonText(settings.getHeroPrimaryButtonText()); hero.setSecondaryButtonText(settings.getHeroSecondaryButtonText()); hero.setImageUrl(resolve(settings.getHeroAssetKey(), settings.getHeroExternalImageUrl()));
        PublicLandingDto.Approach approach = dto.getApproach(); approach.setTitle(settings.getApproachTitle()); approach.setHighlight(settings.getApproachHighlight()); approach.setDescription(settings.getApproachDescription()); approach.setSecondaryDescription(settings.getApproachSecondaryDescription()); approach.setCtaText(settings.getApproachCtaText()); approach.setImageUrl(resolve(settings.getApproachAssetKey(), settings.getApproachExternalImageUrl()));
        PublicLandingDto.Contact contact = dto.getContact(); contact.setHeading(settings.getContactHeading()); contact.setDescription(settings.getContactDescription()); contact.setPhone(settings.getContactPhone()); contact.setWhatsapp(settings.getContactWhatsapp()); contact.setEmail(settings.getContactEmail()); contact.setAddress(settings.getContactAddress()); contact.setHours(settings.getContactHours()); contact.setMapUrl(settings.getMapUrl());
        PublicLandingDto.Social social = dto.getSocial(); social.setFacebook(settings.getFacebookUrl()); social.setInstagram(settings.getInstagramUrl()); social.setTiktok(settings.getTiktokUrl()); social.setLinkedin(settings.getLinkedinUrl());
        PublicLandingDto.Seo seo = dto.getSeo(); seo.setTitle(settings.getSeoTitle()); seo.setDescription(settings.getSeoDescription()); seo.setSiteName(settings.getSeoSiteName()); seo.setImageUrl(resolve(settings.getSeoAssetKey(), settings.getSeoExternalImageUrl()));

        Map<String, List<String>> services = clinicalServiceRepository.findAllByDeletedFalseOrderBySpecialtyAscNameAsc().stream()
                .collect(Collectors.groupingBy(ClinicalService::getSpecialty, Collectors.mapping(ClinicalService::getName, Collectors.toList())));
        specialtyRepository.findAllByOrderByDisplayOrderAscIdAsc().stream().filter(WebsiteSpecialty::isVisible).forEach(item -> {
            PublicLandingDto.Specialty specialty = new PublicLandingDto.Specialty(); specialty.setCode(item.getCode()); specialty.setLabel(item.getLabel()); specialty.setTitle(item.getTitle()); specialty.setSubtitle(item.getSubtitle()); specialty.setIconCode(item.getIconCode());
            specialty.setServices(services.getOrDefault(item.getCode(), List.of())); dto.getSpecialties().add(specialty);
        });
        benefitRepository.findAllByOrderByDisplayOrderAscIdAsc().stream().filter(WebsiteBenefit::isActive).forEach(item -> { PublicLandingDto.Benefit b = new PublicLandingDto.Benefit(); b.setTitle(item.getTitle()); b.setDescription(item.getDescription()); b.setIconCode(item.getIconCode()); dto.getBenefits().add(b); });
        processStepRepository.findAllByOrderByDisplayOrderAscIdAsc().stream().filter(WebsiteProcessStep::isActive).forEach(item -> { PublicLandingDto.ProcessStep p = new PublicLandingDto.ProcessStep(); p.setStepNumber(item.getStepNumber()); p.setTitle(item.getTitle()); p.setDescription(item.getDescription()); dto.getProcessSteps().add(p); });
        professionalRepository.findAllByOrderByDisplayOrderAscIdAsc().stream().filter(WebsiteProfessional::isActive).forEach(item -> { PublicLandingDto.Professional p = new PublicLandingDto.Professional(); p.setName(item.getName()); p.setSpecialty(item.getSpecialty()); p.setLicenseNumber(item.getLicenseNumber()); p.setDescription(item.getDescription()); p.setExperience(item.getExperience()); p.setCareAreas(item.getCareAreas()); p.setPhotoUrl(resolve(item.getPhotoAssetKey(), item.getPhotoExternalUrl())); dto.getProfessionals().add(p); });
        return dto;
    }

    public Resource loadAsset(String key) { return fileStorage.load(key); }

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

        copyDraftSettings(dto, settings);
        reconcileSpecialties(dto);
        reconcileBenefits(dto);
        reconcileProcessSteps(dto);
        reconcileProfessionals(dto);
        promoteSettingsImages(dto, settings);

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
        WebsiteDraftDto dto = buildDraftFromPublished();
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

    private void copyDraftSettings(WebsiteDraftDto d, WebsiteSettings s) {
        s.setCommercialName(d.getCommercialName().trim());
        s.setTagline(trimToNull(d.getTagline()));
        s.setDescription(trimToNull(d.getDescription()));
        s.setLogoExternalImageUrl(trimToNull(d.getLogoExternalImageUrl()));
        s.setHeroEyebrow(trimToNull(d.getHeroEyebrow()));
        s.setHeroTitle(d.getHeroTitle().trim());
        s.setHeroHighlight(trimToNull(d.getHeroHighlight()));
        s.setHeroDescription(trimToNull(d.getHeroDescription()));
        s.setHeroPrimaryButtonText(trimToNull(d.getHeroPrimaryButtonText()));
        s.setHeroSecondaryButtonText(trimToNull(d.getHeroSecondaryButtonText()));
        s.setHeroExternalImageUrl(trimToNull(d.getHeroExternalImageUrl()));
        s.setApproachTitle(trimToNull(d.getApproachTitle()));
        s.setApproachHighlight(trimToNull(d.getApproachHighlight()));
        s.setApproachDescription(trimToNull(d.getApproachDescription()));
        s.setApproachSecondaryDescription(trimToNull(d.getApproachSecondaryDescription()));
        s.setApproachCtaText(trimToNull(d.getApproachCtaText()));
        s.setApproachExternalImageUrl(trimToNull(d.getApproachExternalImageUrl()));
        s.setContactHeading(trimToNull(d.getContactHeading()));
        s.setContactDescription(trimToNull(d.getContactDescription()));
        s.setContactPhone(trimToNull(d.getContactPhone()));
        s.setContactWhatsapp(trimToNull(d.getContactWhatsapp()));
        s.setContactEmail(trimToNull(d.getContactEmail()));
        s.setContactAddress(trimToNull(d.getContactAddress()));
        s.setContactHours(trimToNull(d.getContactHours()));
        s.setMapUrl(trimToNull(d.getMapUrl()));
        s.setFacebookUrl(trimToNull(d.getFacebookUrl()));
        s.setInstagramUrl(trimToNull(d.getInstagramUrl()));
        s.setTiktokUrl(trimToNull(d.getTiktokUrl()));
        s.setLinkedinUrl(trimToNull(d.getLinkedinUrl()));
        s.setSeoTitle(trimToNull(d.getSeoTitle()));
        s.setSeoDescription(trimToNull(d.getSeoDescription()));
        s.setSeoSiteName(trimToNull(d.getSeoSiteName()));
        s.setSeoExternalImageUrl(trimToNull(d.getSeoExternalImageUrl()));
    }

    private void promoteSettingsImages(WebsiteDraftDto d, WebsiteSettings s) {
        String logo = promote(d.getLogoAssetKey());
        s.setLogoAssetKey(logo);
        d.setLogoAssetKey(logo);
        String hero = promote(d.getHeroAssetKey());
        s.setHeroAssetKey(hero);
        d.setHeroAssetKey(hero);
        String approach = promote(d.getApproachAssetKey());
        s.setApproachAssetKey(approach);
        d.setApproachAssetKey(approach);
        String seo = promote(d.getSeoAssetKey());
        s.setSeoAssetKey(seo);
        d.setSeoAssetKey(seo);
    }

    private String promote(String draftKey) {
        if (draftKey == null || draftKey.isBlank()) return null;
        return fileStorage.promote(draftKey);
    }

    private void reconcileSpecialties(WebsiteDraftDto dto) {
        Map<String, WebsiteSpecialty> existing = specialtyRepository.findAll().stream()
                .collect(Collectors.toMap(WebsiteSpecialty::getCode, Function.identity(), (a, b) -> a));
        for (WebsiteDraftDto.Specialty item : dto.getSpecialties()) {
            String code = item.getCode().trim().toUpperCase();
            WebsiteSpecialty entity = existing.get(code);
            if (entity == null) {
                entity = new WebsiteSpecialty();
                entity.setCode(code);
                existing.put(code, entity);
            }
            entity.setLabel(item.getLabel().trim());
            entity.setTitle(item.getTitle().trim());
            entity.setSubtitle(trimToNull(item.getSubtitle()));
            entity.setIconCode(item.getIconCode().trim().toUpperCase());
            entity.setDisplayOrder(item.getDisplayOrder());
            entity.setVisible(item.isVisible());
            specialtyRepository.save(entity);
            item.setId(entity.getId());
            item.setCode(code);
        }
    }

    private void reconcileBenefits(WebsiteDraftDto dto) {
        Map<Long, WebsiteBenefit> existing = benefitRepository.findAll().stream()
                .collect(Collectors.toMap(WebsiteBenefit::getId, Function.identity()));
        Set<Long> kept = new HashSet<>();
        for (WebsiteDraftDto.Benefit item : dto.getBenefits()) {
            WebsiteBenefit entity = item.getId() == null ? null : existing.get(item.getId());
            if (entity == null) entity = new WebsiteBenefit();
            entity.setTitle(item.getTitle().trim());
            entity.setDescription(trimToNull(item.getDescription()));
            entity.setIconCode(item.getIconCode().trim().toUpperCase());
            entity.setDisplayOrder(item.getDisplayOrder());
            entity.setActive(item.isActive());
            benefitRepository.save(entity);
            item.setId(entity.getId());
            kept.add(entity.getId());
        }
        existing.values().stream().filter(b -> !kept.contains(b.getId())).forEach(benefitRepository::delete);
    }

    private void reconcileProcessSteps(WebsiteDraftDto dto) {
        Map<Long, WebsiteProcessStep> existing = processStepRepository.findAll().stream()
                .collect(Collectors.toMap(WebsiteProcessStep::getId, Function.identity()));
        Set<Long> kept = new HashSet<>();
        for (WebsiteDraftDto.ProcessStep item : dto.getProcessSteps()) {
            WebsiteProcessStep entity = item.getId() == null ? null : existing.get(item.getId());
            if (entity == null) entity = new WebsiteProcessStep();
            entity.setStepNumber(item.getStepNumber());
            entity.setTitle(item.getTitle().trim());
            entity.setDescription(trimToNull(item.getDescription()));
            entity.setDisplayOrder(item.getDisplayOrder());
            entity.setActive(item.isActive());
            processStepRepository.save(entity);
            item.setId(entity.getId());
            kept.add(entity.getId());
        }
        existing.values().stream().filter(p -> !kept.contains(p.getId())).forEach(processStepRepository::delete);
    }

    private void reconcileProfessionals(WebsiteDraftDto dto) {
        Map<Long, WebsiteProfessional> existing = professionalRepository.findAll().stream()
                .collect(Collectors.toMap(WebsiteProfessional::getId, Function.identity()));
        Set<Long> kept = new HashSet<>();
        for (WebsiteDraftDto.Professional item : dto.getProfessionals()) {
            WebsiteProfessional entity = item.getId() == null ? null : existing.get(item.getId());
            if (entity == null) entity = new WebsiteProfessional();
            entity.setName(item.getName().trim());
            entity.setSpecialty(trimToNull(item.getSpecialty()));
            entity.setLicenseNumber(trimToNull(item.getLicenseNumber()));
            entity.setDescription(trimToNull(item.getDescription()));
            entity.setExperience(trimToNull(item.getExperience()));
            entity.setCareAreas(trimToNull(item.getCareAreas()));
            entity.setPhotoExternalUrl(trimToNull(item.getPhotoExternalUrl()));
            String publishedPhoto = promote(item.getPhotoAssetKey());
            entity.setPhotoAssetKey(trimToNull(publishedPhoto));
            entity.setDisplayOrder(item.getDisplayOrder());
            entity.setActive(item.isActive());
            professionalRepository.save(entity);
            item.setId(entity.getId());
            item.setPhotoAssetKey(trimToNull(publishedPhoto));
            kept.add(entity.getId());
        }
        existing.values().stream().filter(p -> !kept.contains(p.getId())).forEach(professionalRepository::delete);
    }

    private WebsiteDraftDto buildDraftFromPublished() {
        WebsiteSettings settings = requireSettings();
        WebsiteDraftDto dto = new WebsiteDraftDto();
        dto.setCommercialName(settings.getCommercialName());
        dto.setTagline(settings.getTagline());
        dto.setDescription(settings.getDescription());
        dto.setLogoExternalImageUrl(settings.getLogoExternalImageUrl());
        dto.setLogoAssetKey(settings.getLogoAssetKey());
        dto.setHeroEyebrow(settings.getHeroEyebrow());
        dto.setHeroTitle(settings.getHeroTitle());
        dto.setHeroHighlight(settings.getHeroHighlight());
        dto.setHeroDescription(settings.getHeroDescription());
        dto.setHeroPrimaryButtonText(settings.getHeroPrimaryButtonText());
        dto.setHeroSecondaryButtonText(settings.getHeroSecondaryButtonText());
        dto.setHeroExternalImageUrl(settings.getHeroExternalImageUrl());
        dto.setHeroAssetKey(settings.getHeroAssetKey());
        dto.setApproachTitle(settings.getApproachTitle());
        dto.setApproachHighlight(settings.getApproachHighlight());
        dto.setApproachDescription(settings.getApproachDescription());
        dto.setApproachSecondaryDescription(settings.getApproachSecondaryDescription());
        dto.setApproachCtaText(settings.getApproachCtaText());
        dto.setApproachExternalImageUrl(settings.getApproachExternalImageUrl());
        dto.setApproachAssetKey(settings.getApproachAssetKey());
        dto.setContactHeading(settings.getContactHeading());
        dto.setContactDescription(settings.getContactDescription());
        dto.setContactPhone(settings.getContactPhone());
        dto.setContactWhatsapp(settings.getContactWhatsapp());
        dto.setContactEmail(settings.getContactEmail());
        dto.setContactAddress(settings.getContactAddress());
        dto.setContactHours(settings.getContactHours());
        dto.setMapUrl(settings.getMapUrl());
        dto.setFacebookUrl(settings.getFacebookUrl());
        dto.setInstagramUrl(settings.getInstagramUrl());
        dto.setTiktokUrl(settings.getTiktokUrl());
        dto.setLinkedinUrl(settings.getLinkedinUrl());
        dto.setSeoTitle(settings.getSeoTitle());
        dto.setSeoDescription(settings.getSeoDescription());
        dto.setSeoSiteName(settings.getSeoSiteName());
        dto.setSeoExternalImageUrl(settings.getSeoExternalImageUrl());
        dto.setSeoAssetKey(settings.getSeoAssetKey());

        specialtyRepository.findAllByOrderByDisplayOrderAscIdAsc().forEach(s -> {
            WebsiteDraftDto.Specialty x = new WebsiteDraftDto.Specialty();
            x.setDraftKey(UUID.randomUUID().toString());
            x.setId(s.getId());
            x.setCode(s.getCode());
            x.setLabel(s.getLabel());
            x.setTitle(s.getTitle());
            x.setSubtitle(s.getSubtitle());
            x.setIconCode(s.getIconCode());
            x.setDisplayOrder(s.getDisplayOrder());
            x.setVisible(s.isVisible());
            dto.getSpecialties().add(x);
        });
        benefitRepository.findAllByOrderByDisplayOrderAscIdAsc().forEach(b -> {
            WebsiteDraftDto.Benefit x = new WebsiteDraftDto.Benefit();
            x.setDraftKey(UUID.randomUUID().toString());
            x.setId(b.getId());
            x.setTitle(b.getTitle());
            x.setDescription(b.getDescription());
            x.setIconCode(b.getIconCode());
            x.setDisplayOrder(b.getDisplayOrder());
            x.setActive(b.isActive());
            dto.getBenefits().add(x);
        });
        processStepRepository.findAllByOrderByDisplayOrderAscIdAsc().forEach(p -> {
            WebsiteDraftDto.ProcessStep x = new WebsiteDraftDto.ProcessStep();
            x.setDraftKey(UUID.randomUUID().toString());
            x.setId(p.getId());
            x.setStepNumber(p.getStepNumber());
            x.setTitle(p.getTitle());
            x.setDescription(p.getDescription());
            x.setDisplayOrder(p.getDisplayOrder());
            x.setActive(p.isActive());
            dto.getProcessSteps().add(x);
        });
        professionalRepository.findAllByOrderByDisplayOrderAscIdAsc().forEach(p -> {
            WebsiteDraftDto.Professional x = new WebsiteDraftDto.Professional();
            x.setDraftKey(UUID.randomUUID().toString());
            x.setId(p.getId());
            x.setName(p.getName());
            x.setSpecialty(p.getSpecialty());
            x.setLicenseNumber(p.getLicenseNumber());
            x.setDescription(p.getDescription());
            x.setExperience(p.getExperience());
            x.setCareAreas(p.getCareAreas());
            x.setPhotoExternalUrl(p.getPhotoExternalUrl());
            x.setPhotoAssetKey(p.getPhotoAssetKey());
            x.setDisplayOrder(p.getDisplayOrder());
            x.setActive(p.isActive());
            dto.getProfessionals().add(x);
        });
        return dto;
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

    private String resolve(String assetKey, String externalUrl) { return assetKey == null || assetKey.isBlank() ? externalUrl : fileStorage.publicUrl(assetKey); }
    private String trimToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
