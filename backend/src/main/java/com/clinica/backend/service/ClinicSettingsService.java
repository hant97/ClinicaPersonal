package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicSettingsDto;
import com.clinica.backend.model.ClinicSettings;
import com.clinica.backend.repository.ClinicSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicSettingsService {

    private final ClinicSettingsRepository repository;
    private final String UPLOAD_DIR = "uploads/logos/";

    public ClinicSettingsDto getSettings() {
        ClinicSettings settings = repository.findTopByDeletedFalseOrderByIdAsc()
                .orElseGet(this::createDefaultSettings);
        return mapToDto(settings);
    }

    public ClinicSettingsDto updateSettings(ClinicSettingsDto dto) {
        ClinicSettings settings = repository.findTopByDeletedFalseOrderByIdAsc()
                .orElseGet(this::createDefaultSettings);
        
        settings.setClinicName(dto.getClinicName());
        settings.setShortName(dto.getShortName());
        settings.setContactEmail(dto.getContactEmail());
        settings.setContactPhone(dto.getContactPhone());
        settings.setAddress(dto.getAddress());
        
        ClinicSettings saved = repository.save(settings);
        return mapToDto(saved);
    }

    public ClinicSettingsDto uploadLogo(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            ClinicSettings settings = repository.findTopByDeletedFalseOrderByIdAsc()
                    .orElseGet(this::createDefaultSettings);
            
            String logoUrl = "/api/settings/clinic/logo/" + filename;
            settings.setLogoUrl(logoUrl);
            repository.save(settings);
            
            return mapToDto(settings);
        } catch (IOException e) {
            throw new RuntimeException("Could not store the logo file. Error: " + e.getMessage());
        }
    }

    private ClinicSettings createDefaultSettings() {
        ClinicSettings settings = new ClinicSettings();
        settings.setClinicName("Mi Clínica");
        settings.setShortName("Clínica");
        return repository.save(settings);
    }

    private ClinicSettingsDto mapToDto(ClinicSettings entity) {
        ClinicSettingsDto dto = new ClinicSettingsDto();
        dto.setId(entity.getId());
        dto.setClinicName(entity.getClinicName());
        dto.setShortName(entity.getShortName());
        dto.setLogoUrl(entity.getLogoUrl());
        dto.setContactEmail(entity.getContactEmail());
        dto.setContactPhone(entity.getContactPhone());
        dto.setAddress(entity.getAddress());
        return dto;
    }
}
