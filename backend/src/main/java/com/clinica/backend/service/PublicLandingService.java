package com.clinica.backend.service;

import com.clinica.backend.dto.PublicLandingDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.model.WebsiteBenefit;
import com.clinica.backend.model.WebsiteProcessStep;
import com.clinica.backend.model.WebsiteProfessional;
import com.clinica.backend.model.WebsiteSettings;
import com.clinica.backend.model.WebsiteSpecialty;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.WebsiteBenefitRepository;
import com.clinica.backend.repository.WebsiteProcessStepRepository;
import com.clinica.backend.repository.WebsiteProfessionalRepository;
import com.clinica.backend.repository.WebsiteSettingsRepository;
import com.clinica.backend.repository.WebsiteSpecialtyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Arma el contenido público de la landing (visitantes anónimos) y sirve los
 * assets publicados. Es de solo lectura: no participa del flujo de edición
 * ni de publicación del borrador (ver {@link WebsiteSettingsService} y
 * {@link WebsiteDraftPublishService}).
 */
@Service
@RequiredArgsConstructor
public class PublicLandingService {
    private static final String SINGLETON_KEY = "S";

    private final WebsiteSettingsRepository settingsRepository;
    private final ClinicalServiceRepository clinicalServiceRepository;
    private final WebsiteSpecialtyRepository specialtyRepository;
    private final WebsiteBenefitRepository benefitRepository;
    private final WebsiteProcessStepRepository processStepRepository;
    private final WebsiteProfessionalRepository professionalRepository;
    private final WebsiteFileStorage fileStorage;

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

    public Resource loadAsset(String key) {
        // Las fotos de pacientes se guardaban aquí antes de V14; nunca deben servirse sin autenticación.
        if (key == null || key.startsWith("patients/")) {
            throw new ResourceNotFoundException("Archivo no encontrado");
        }
        return fileStorage.load(key);
    }

    private String resolve(String assetKey, String externalUrl) {
        return assetKey == null || assetKey.isBlank() ? externalUrl : fileStorage.publicUrl(assetKey);
    }
}
