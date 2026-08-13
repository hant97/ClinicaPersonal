package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicSettingsDto;
import com.clinica.backend.exception.BusinessRuleException;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicSettingsService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".webp", ".svg");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/png", "image/jpeg", "image/webp", "image/svg+xml");
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

    private final ClinicSettingsRepository repository;
    private final String UPLOAD_DIR = "uploads/logos/";

    public ClinicSettingsDto getSettings(String specialty) {
        ClinicSettings settings = repository.findTopBySpecialtyAndDeletedFalseOrderByIdAsc(specialty)
                .orElseGet(() -> createDefaultSettings(specialty));
        return mapToDto(settings);
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
        return mapToDto(saved);
    }

    public ClinicSettingsDto uploadLogo(MultipartFile file, String specialty) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo del logo no puede estar vacío");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El tamaño del archivo no debe exceder 2MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Tipo de imagen no permitido. Solo se aceptan PNG, JPG, WEBP y SVG.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf('.') != -1) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Extensión de archivo no permitida.");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadPath)) {
                throw new IllegalArgumentException("Nombre de archivo inválido");
            }
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            ClinicSettings settings = repository.findTopBySpecialtyAndDeletedFalseOrderByIdAsc(specialty)
                    .orElseGet(() -> createDefaultSettings(specialty));
            
            String logoUrl = "/api/settings/clinic/logo/" + filename;
            settings.setLogoUrl(logoUrl);
            repository.save(settings);
            
            return mapToDto(settings);
        } catch (IOException e) {
            throw new BusinessRuleException("No se pudo almacenar el archivo del logo");
        }
    }

    private ClinicSettings createDefaultSettings(String specialty) {
        ClinicSettings settings = new ClinicSettings();
        settings.setClinicName("Mi Clínica");
        settings.setShortName("Clínica");
        settings.setSpecialty(specialty);
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
        dto.setSpecialty(entity.getSpecialty());
        return dto;
    }
}
