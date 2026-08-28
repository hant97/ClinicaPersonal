package com.clinica.backend.service;

import com.clinica.backend.dto.WebsiteDraftDto;
import com.clinica.backend.model.WebsiteBenefit;
import com.clinica.backend.model.WebsiteProcessStep;
import com.clinica.backend.model.WebsiteProfessional;
import com.clinica.backend.model.WebsiteSettings;
import com.clinica.backend.model.WebsiteSpecialty;
import com.clinica.backend.repository.WebsiteBenefitRepository;
import com.clinica.backend.repository.WebsiteProcessStepRepository;
import com.clinica.backend.repository.WebsiteProfessionalRepository;
import com.clinica.backend.repository.WebsiteSpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Traduce el borrador del editor visual ({@link WebsiteDraftDto}) a las
 * entidades persistidas de la landing pública cuando {@code publish()} se
 * ejecuta desde {@link WebsiteSettingsService}, y reconstruye un borrador
 * nuevo a partir de lo publicado cuando se descarta el borrador actual
 * ({@code resetDraft()}).
 * <p>
 * Se mantiene separado de {@link WebsiteSettingsService} porque su única
 * razón de cambio es cómo se reconcilian los datos publicados, no cómo se
 * gestiona la sesión de edición (revisiones, assets del borrador, etc.).
 */
@Service
@RequiredArgsConstructor
public class WebsiteDraftPublishService {

    private final WebsiteSpecialtyRepository specialtyRepository;
    private final WebsiteBenefitRepository benefitRepository;
    private final WebsiteProcessStepRepository processStepRepository;
    private final WebsiteProfessionalRepository professionalRepository;
    private final WebsiteFileStorage fileStorage;

    public void applyDraftToSettings(WebsiteDraftDto d, WebsiteSettings s) {
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

    public void promoteSettingsImages(WebsiteDraftDto d, WebsiteSettings s) {
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

    public void reconcileSpecialties(WebsiteDraftDto dto) {
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

    public void reconcileBenefits(WebsiteDraftDto dto) {
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

    public void reconcileProcessSteps(WebsiteDraftDto dto) {
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

    public void reconcileProfessionals(WebsiteDraftDto dto) {
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

    public WebsiteDraftDto buildDraftFromPublished(WebsiteSettings settings) {
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

    private String promote(String draftKey) {
        if (draftKey == null || draftKey.isBlank()) return null;
        return fileStorage.promote(draftKey);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
