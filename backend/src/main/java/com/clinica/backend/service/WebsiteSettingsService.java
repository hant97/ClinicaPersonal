package com.clinica.backend.service;

import com.clinica.backend.dto.PublicLandingDto;
import com.clinica.backend.dto.WebsiteSettingsAdminDto;
import com.clinica.backend.model.WebsiteBenefit;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.model.WebsiteProcessStep;
import com.clinica.backend.model.WebsiteProfessional;
import com.clinica.backend.model.WebsiteSettings;
import com.clinica.backend.model.WebsiteSpecialty;
import com.clinica.backend.model.WebsiteSpecialtyService;
import com.clinica.backend.repository.WebsiteBenefitRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.WebsiteProcessStepRepository;
import com.clinica.backend.repository.WebsiteProfessionalRepository;
import com.clinica.backend.repository.WebsiteSettingsRepository;
import com.clinica.backend.repository.WebsiteSpecialtyRepository;
import com.clinica.backend.repository.WebsiteSpecialtyServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebsiteSettingsService {
    private static final String SINGLETON_KEY = "S";
    private static final Set<String> ALLOWED_ICON_CODES = Set.of("HEART_HANDSHAKE", "SHIELD_CHECK", "STETHOSCOPE", "SPARKLES", "MICROSCOPE", "BRAIN");
    private final WebsiteSettingsRepository settingsRepository;
    private final ClinicalServiceRepository clinicalServiceRepository;
    private final WebsiteSpecialtyRepository specialtyRepository;
    private final WebsiteSpecialtyServiceRepository specialtyServiceRepository;
    private final WebsiteBenefitRepository benefitRepository;
    private final WebsiteProcessStepRepository processStepRepository;
    private final WebsiteProfessionalRepository professionalRepository;
    private final WebsiteFileStorage fileStorage;

    @Transactional
    public WebsiteSettingsAdminDto getAdminSettings() {
        WebsiteSettings settings = settingsRepository.findBySingletonKey(SINGLETON_KEY).orElseThrow();
        WebsiteSettingsAdminDto dto = mapAdmin(settings);
        dto.setLogoExternalImageUrl(settings.getLogoExternalImageUrl());
        dto.setLogoAssetKey(settings.getLogoAssetKey());
        return dto;
    }

    @Transactional
    public WebsiteSettingsAdminDto update(WebsiteSettingsAdminDto dto) {
        validateUrls(dto);
        validateIconCodes(dto);
        WebsiteSettings settings = settingsRepository.findBySingletonKey(SINGLETON_KEY).orElseThrow();
        copySettings(dto, settings);
        settingsRepository.save(settings);

        specialtyServiceRepository.deleteAllInBatch();
        specialtyRepository.deleteAllInBatch();
        for (WebsiteSettingsAdminDto.Specialty item : dto.getSpecialties()) {
            WebsiteSpecialty specialty = new WebsiteSpecialty();
            specialty.setCode(item.getCode().trim().toUpperCase());
            specialty.setLabel(item.getLabel().trim());
            specialty.setTitle(item.getTitle().trim());
            specialty.setSubtitle(trimToNull(item.getSubtitle()));
            specialty.setIconCode(item.getIconCode().trim().toUpperCase());
            specialty.setDisplayOrder(item.getDisplayOrder());
            specialty.setVisible(item.isVisible());
            specialtyRepository.save(specialty);
        }

        benefitRepository.deleteAllInBatch();
        dto.getBenefits().forEach(item -> {
            WebsiteBenefit benefit = new WebsiteBenefit();
            benefit.setTitle(item.getTitle().trim()); benefit.setDescription(trimToNull(item.getDescription()));
            benefit.setIconCode(item.getIconCode().trim().toUpperCase()); benefit.setDisplayOrder(item.getDisplayOrder()); benefit.setActive(item.isActive());
            benefitRepository.save(benefit);
        });

        processStepRepository.deleteAllInBatch();
        dto.getProcessSteps().forEach(item -> {
            WebsiteProcessStep step = new WebsiteProcessStep();
            step.setStepNumber(item.getStepNumber()); step.setTitle(item.getTitle().trim()); step.setDescription(trimToNull(item.getDescription()));
            step.setDisplayOrder(item.getDisplayOrder()); step.setActive(item.isActive()); processStepRepository.save(step);
        });

        professionalRepository.deleteAllInBatch();
        dto.getProfessionals().forEach(item -> {
            WebsiteProfessional professional = new WebsiteProfessional();
            professional.setName(item.getName().trim()); professional.setSpecialty(trimToNull(item.getSpecialty()));
            professional.setLicenseNumber(trimToNull(item.getLicenseNumber())); professional.setDescription(trimToNull(item.getDescription()));
            professional.setExperience(trimToNull(item.getExperience())); professional.setCareAreas(trimToNull(item.getCareAreas()));
            professional.setPhotoExternalUrl(trimToNull(item.getPhotoExternalUrl())); professional.setPhotoAssetKey(trimToNull(item.getPhotoAssetKey()));
            professional.setDisplayOrder(item.getDisplayOrder()); professional.setActive(item.isActive()); professionalRepository.save(professional);
        });
        return getAdminSettings();
    }

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

    public org.springframework.core.io.Resource loadAsset(String key) { return fileStorage.load(key); }

    @Transactional
    public WebsiteSettingsAdminDto uploadAsset(String category, org.springframework.web.multipart.MultipartFile file) {
        if (!List.of("logo", "hero", "approach", "seo").contains(category)) throw new IllegalArgumentException("Tipo de imagen no permitido");
        WebsiteSettings settings = settingsRepository.findBySingletonKey(SINGLETON_KEY).orElseThrow();
        String previous = switch (category) { case "logo" -> settings.getLogoAssetKey(); case "hero" -> settings.getHeroAssetKey(); case "approach" -> settings.getApproachAssetKey(); default -> settings.getSeoAssetKey(); };
        String key = fileStorage.store(file, category, previous);
        switch (category) { case "logo" -> settings.setLogoAssetKey(key); case "hero" -> settings.setHeroAssetKey(key); case "approach" -> settings.setApproachAssetKey(key); default -> settings.setSeoAssetKey(key); }
        settingsRepository.save(settings);
        return getAdminSettings();
    }

    @Transactional
    public WebsiteSettingsAdminDto deleteAsset(String category) {
        WebsiteSettings settings = settingsRepository.findBySingletonKey(SINGLETON_KEY).orElseThrow();
        String previous = switch (category) { case "logo" -> settings.getLogoAssetKey(); case "hero" -> settings.getHeroAssetKey(); case "approach" -> settings.getApproachAssetKey(); case "seo" -> settings.getSeoAssetKey(); default -> throw new IllegalArgumentException("Tipo de imagen no permitido"); };
        fileStorage.delete(previous);
        switch (category) { case "logo" -> settings.setLogoAssetKey(null); case "hero" -> settings.setHeroAssetKey(null); case "approach" -> settings.setApproachAssetKey(null); default -> settings.setSeoAssetKey(null); }
        settingsRepository.save(settings);
        return getAdminSettings();
    }

    @Transactional
    public WebsiteSettingsAdminDto uploadProfessionalAsset(Long professionalId, org.springframework.web.multipart.MultipartFile file) {
        WebsiteProfessional professional = professionalRepository.findById(professionalId).orElseThrow();
        String key = fileStorage.store(file, "professionals", professional.getPhotoAssetKey());
        professional.setPhotoAssetKey(key);
        professionalRepository.save(professional);
        return getAdminSettings();
    }

    private String resolve(String assetKey, String externalUrl) { return assetKey == null || assetKey.isBlank() ? externalUrl : fileStorage.publicUrl(assetKey); }
    private String trimToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private void copySettings(WebsiteSettingsAdminDto d, WebsiteSettings s) {
        s.setLogoExternalImageUrl(trimToNull(d.getLogoExternalImageUrl()));
        if (d.getLogoAssetKey() != null) s.setLogoAssetKey(trimToNull(d.getLogoAssetKey()));
        s.setCommercialName(d.getCommercialName().trim()); s.setTagline(trimToNull(d.getTagline())); s.setDescription(trimToNull(d.getDescription())); s.setHeroEyebrow(trimToNull(d.getHeroEyebrow())); s.setHeroTitle(d.getHeroTitle().trim()); s.setHeroHighlight(trimToNull(d.getHeroHighlight())); s.setHeroDescription(trimToNull(d.getHeroDescription())); s.setHeroPrimaryButtonText(trimToNull(d.getHeroPrimaryButtonText())); s.setHeroSecondaryButtonText(trimToNull(d.getHeroSecondaryButtonText())); s.setHeroExternalImageUrl(trimToNull(d.getHeroExternalImageUrl())); s.setHeroAssetKey(trimToNull(d.getHeroAssetKey())); s.setApproachTitle(trimToNull(d.getApproachTitle())); s.setApproachHighlight(trimToNull(d.getApproachHighlight())); s.setApproachDescription(trimToNull(d.getApproachDescription())); s.setApproachSecondaryDescription(trimToNull(d.getApproachSecondaryDescription())); s.setApproachCtaText(trimToNull(d.getApproachCtaText())); s.setApproachExternalImageUrl(trimToNull(d.getApproachExternalImageUrl())); s.setApproachAssetKey(trimToNull(d.getApproachAssetKey())); s.setContactHeading(trimToNull(d.getContactHeading())); s.setContactDescription(trimToNull(d.getContactDescription())); s.setContactPhone(trimToNull(d.getContactPhone())); s.setContactWhatsapp(trimToNull(d.getContactWhatsapp())); s.setContactEmail(trimToNull(d.getContactEmail())); s.setContactAddress(trimToNull(d.getContactAddress())); s.setContactHours(trimToNull(d.getContactHours())); s.setMapUrl(trimToNull(d.getMapUrl())); s.setFacebookUrl(trimToNull(d.getFacebookUrl())); s.setInstagramUrl(trimToNull(d.getInstagramUrl())); s.setTiktokUrl(trimToNull(d.getTiktokUrl())); s.setLinkedinUrl(trimToNull(d.getLinkedinUrl())); s.setSeoTitle(trimToNull(d.getSeoTitle())); s.setSeoDescription(trimToNull(d.getSeoDescription())); s.setSeoSiteName(trimToNull(d.getSeoSiteName())); s.setSeoExternalImageUrl(trimToNull(d.getSeoExternalImageUrl())); s.setSeoAssetKey(trimToNull(d.getSeoAssetKey()));
    }
    private void validateUrls(WebsiteSettingsAdminDto dto) { List<String> urls = java.util.Arrays.asList(dto.getHeroExternalImageUrl(), dto.getApproachExternalImageUrl(), dto.getSeoExternalImageUrl(), dto.getMapUrl(), dto.getFacebookUrl(), dto.getInstagramUrl(), dto.getTiktokUrl(), dto.getLinkedinUrl()); urls.forEach(this::validateUrl); dto.getProfessionals().forEach(p -> validateUrl(p.getPhotoExternalUrl())); }
    private void validateIconCodes(WebsiteSettingsAdminDto dto) { dto.getSpecialties().forEach(s -> validateIcon(s.getIconCode())); dto.getBenefits().forEach(b -> validateIcon(b.getIconCode())); }
    private void validateIcon(String code) { if (code == null || !ALLOWED_ICON_CODES.contains(code.trim().toUpperCase())) throw new IllegalArgumentException("El icono seleccionado no está permitido"); }
    private void validateUrl(String value) { if (value == null || value.isBlank()) return; URI uri; try { uri = URI.create(value.trim()); } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Una URL del sitio no es válida"); } if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) throw new IllegalArgumentException("Las URLs deben usar http o https"); }
    private WebsiteSettingsAdminDto mapAdmin(WebsiteSettings settings) { WebsiteSettingsAdminDto d = new WebsiteSettingsAdminDto(); d.setId(settings.getId()); d.setCommercialName(settings.getCommercialName()); d.setTagline(settings.getTagline()); d.setDescription(settings.getDescription()); d.setHeroEyebrow(settings.getHeroEyebrow()); d.setHeroTitle(settings.getHeroTitle()); d.setHeroHighlight(settings.getHeroHighlight()); d.setHeroDescription(settings.getHeroDescription()); d.setHeroPrimaryButtonText(settings.getHeroPrimaryButtonText()); d.setHeroSecondaryButtonText(settings.getHeroSecondaryButtonText()); d.setHeroExternalImageUrl(settings.getHeroExternalImageUrl()); d.setHeroAssetKey(settings.getHeroAssetKey()); d.setApproachTitle(settings.getApproachTitle()); d.setApproachHighlight(settings.getApproachHighlight()); d.setApproachDescription(settings.getApproachDescription()); d.setApproachSecondaryDescription(settings.getApproachSecondaryDescription()); d.setApproachCtaText(settings.getApproachCtaText()); d.setApproachExternalImageUrl(settings.getApproachExternalImageUrl()); d.setApproachAssetKey(settings.getApproachAssetKey()); d.setContactHeading(settings.getContactHeading()); d.setContactDescription(settings.getContactDescription()); d.setContactPhone(settings.getContactPhone()); d.setContactWhatsapp(settings.getContactWhatsapp()); d.setContactEmail(settings.getContactEmail()); d.setContactAddress(settings.getContactAddress()); d.setContactHours(settings.getContactHours()); d.setMapUrl(settings.getMapUrl()); d.setFacebookUrl(settings.getFacebookUrl()); d.setInstagramUrl(settings.getInstagramUrl()); d.setTiktokUrl(settings.getTiktokUrl()); d.setLinkedinUrl(settings.getLinkedinUrl()); d.setSeoTitle(settings.getSeoTitle()); d.setSeoDescription(settings.getSeoDescription()); d.setSeoSiteName(settings.getSeoSiteName()); d.setSeoExternalImageUrl(settings.getSeoExternalImageUrl()); d.setSeoAssetKey(settings.getSeoAssetKey()); specialtyRepository.findAllByOrderByDisplayOrderAscIdAsc().forEach(s -> { WebsiteSettingsAdminDto.Specialty x = new WebsiteSettingsAdminDto.Specialty(); x.setId(s.getId()); x.setCode(s.getCode()); x.setLabel(s.getLabel()); x.setTitle(s.getTitle()); x.setSubtitle(s.getSubtitle()); x.setIconCode(s.getIconCode()); x.setDisplayOrder(s.getDisplayOrder()); x.setVisible(s.isVisible()); specialtyServiceRepository.findAllByOrderByDisplayOrderAscIdAsc().stream().filter(child -> child.getSpecialty().getId().equals(s.getId())).forEach(child -> { WebsiteSettingsAdminDto.SpecialtyService c = new WebsiteSettingsAdminDto.SpecialtyService(); c.setId(child.getId()); c.setName(child.getName()); c.setDisplayOrder(child.getDisplayOrder()); c.setActive(child.isActive()); x.getServices().add(c); }); d.getSpecialties().add(x); }); benefitRepository.findAllByOrderByDisplayOrderAscIdAsc().forEach(b -> { WebsiteSettingsAdminDto.Benefit x = new WebsiteSettingsAdminDto.Benefit(); x.setId(b.getId()); x.setTitle(b.getTitle()); x.setDescription(b.getDescription()); x.setIconCode(b.getIconCode()); x.setDisplayOrder(b.getDisplayOrder()); x.setActive(b.isActive()); d.getBenefits().add(x); }); processStepRepository.findAllByOrderByDisplayOrderAscIdAsc().forEach(p -> { WebsiteSettingsAdminDto.ProcessStep x = new WebsiteSettingsAdminDto.ProcessStep(); x.setId(p.getId()); x.setStepNumber(p.getStepNumber()); x.setTitle(p.getTitle()); x.setDescription(p.getDescription()); x.setDisplayOrder(p.getDisplayOrder()); x.setActive(p.isActive()); d.getProcessSteps().add(x); }); professionalRepository.findAllByOrderByDisplayOrderAscIdAsc().forEach(p -> { WebsiteSettingsAdminDto.Professional x = new WebsiteSettingsAdminDto.Professional(); x.setId(p.getId()); x.setName(p.getName()); x.setSpecialty(p.getSpecialty()); x.setLicenseNumber(p.getLicenseNumber()); x.setDescription(p.getDescription()); x.setExperience(p.getExperience()); x.setCareAreas(p.getCareAreas()); x.setPhotoExternalUrl(p.getPhotoExternalUrl()); x.setPhotoAssetKey(p.getPhotoAssetKey()); x.setDisplayOrder(p.getDisplayOrder()); x.setActive(p.isActive()); d.getProfessionals().add(x); }); return d; }
}
