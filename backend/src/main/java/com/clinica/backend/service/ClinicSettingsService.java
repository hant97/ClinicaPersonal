package com.clinica.backend.service;

import com.clinica.backend.mapper.ClinicSettingsMapper;

import com.clinica.backend.dto.ClinicSettingsDto;
import com.clinica.backend.model.ClinicSettings;
import com.clinica.backend.repository.ClinicSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ClinicSettingsService {

    private static final String LOGO_URL_PREFIX = "/api/v1/settings/clinic/logo/";

    private final ClinicSettingsRepository repository;
    private final ClinicSettingsMapper clinicSettingsMapper;
    private final LogoFileStorage logoStorage;

    public ClinicSettingsDto getSettings(String specialty) {
        ClinicSettings settings = repository.findTopBySpecialtyAndDeletedFalseOrderByIdAsc(specialty)
                .orElseGet(() -> createDefaultSettings(specialty));
        return clinicSettingsMapper.toDto(settings);
    }

    public ClinicSettingsDto updateSettings(ClinicSettingsDto dto, String specialty) {
        ClinicSettings settings = repository.findTopBySpecialtyAndDeletedFalseOrderByIdAsc(specialty)
                .orElseGet(() -> createDefaultSettings(specialty));
        
        settings.setClinicName(dto.getClinicName());
        settings.setShortName(dto.getShortName());
        settings.setContactEmail(dto.getContactEmail());
        settings.setContactPhone(dto.getContactPhone());
        settings.setAddress(dto.getAddress());
        settings.setSpecialty(specialty);
        
        ClinicSettings saved = repository.save(settings);
        return clinicSettingsMapper.toDto(saved);
    }

    public ClinicSettingsDto uploadLogo(MultipartFile file, String specialty) {
        String filename = logoStorage.store(file);

        ClinicSettings settings = repository.findTopBySpecialtyAndDeletedFalseOrderByIdAsc(specialty)
                .orElseGet(() -> createDefaultSettings(specialty));
        settings.setLogoUrl(LOGO_URL_PREFIX + filename);
        repository.save(settings);

        return clinicSettingsMapper.toDto(settings);
    }

    public Resource loadLogo(String filename) {
        return logoStorage.load(filename);
    }

    private ClinicSettings createDefaultSettings(String specialty) {
        ClinicSettings settings = new ClinicSettings();
        settings.setClinicName("Mi Clínica");
        settings.setShortName("Clínica");
        settings.setSpecialty(specialty);
        return repository.save(settings);
    }

}
