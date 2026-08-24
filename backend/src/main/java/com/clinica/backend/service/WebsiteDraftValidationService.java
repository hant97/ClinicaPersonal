package com.clinica.backend.service;

import com.clinica.backend.dto.WebsiteDraftDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WebsiteDraftValidationService {
    private static final Set<String> ALLOWED_ICON_CODES = Set.of(
            "HEART_HANDSHAKE", "SHIELD_CHECK", "STETHOSCOPE", "SPARKLES", "MICROSCOPE", "BRAIN");

    private final Validator validator;

    public void validate(WebsiteDraftDto dto) {
        Set<ConstraintViolation<WebsiteDraftDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        List<String> urls = Arrays.asList(
                dto.getHeroExternalImageUrl(),
                dto.getApproachExternalImageUrl(),
                dto.getSeoExternalImageUrl(),
                dto.getMapUrl(),
                dto.getFacebookUrl(),
                dto.getInstagramUrl(),
                dto.getTiktokUrl(),
                dto.getLinkedinUrl());
        urls.forEach(this::validateUrl);
        dto.getProfessionals().forEach(professional -> validateUrl(professional.getPhotoExternalUrl()));
        dto.getSpecialties().forEach(specialty -> validateIcon(specialty.getIconCode()));
        dto.getBenefits().forEach(benefit -> validateIcon(benefit.getIconCode()));
    }

    private void validateIcon(String code) {
        if (code == null || !ALLOWED_ICON_CODES.contains(code.trim().toUpperCase())) {
            throw new IllegalArgumentException("El icono seleccionado no está permitido");
        }
    }

    private void validateUrl(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Una URL del sitio no es válida");
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("Las URLs deben usar http o https");
        }
    }
}
