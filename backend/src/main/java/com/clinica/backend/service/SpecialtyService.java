package com.clinica.backend.service;

import com.clinica.backend.dto.SpecialtyDto;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Specialty;
import com.clinica.backend.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    @Transactional(readOnly = true)
    public List<SpecialtyDto> getActiveSpecialties() {
        return specialtyRepository.findAllByActiveTrueOrderByDisplayOrderAscCodeAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialtyDto> getAllSpecialties() {
        return specialtyRepository.findAllByOrderByDisplayOrderAscCodeAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpecialtyDto getSpecialtyByCode(String code) {
        Specialty specialty = specialtyRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con código: " + code));
        return toDto(specialty);
    }

    @Transactional
    public SpecialtyDto createSpecialty(SpecialtyDto dto) {
        String normalizedCode = dto.getCode().trim().toUpperCase();
        if (specialtyRepository.existsByCode(normalizedCode)) {
            throw new ConflictException("Ya existe una especialidad registrada con el código: " + normalizedCode);
        }

        Specialty specialty = new Specialty();
        specialty.setCode(normalizedCode);
        specialty.setName(dto.getName().trim());
        specialty.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        specialty.setIcon(dto.getIcon() != null && !dto.getIcon().isBlank() ? dto.getIcon().trim() : "Sparkles");
        specialty.setActive(dto.isActive());
        specialty.setDisplayOrder(dto.getDisplayOrder());

        return toDto(specialtyRepository.save(specialty));
    }

    @Transactional
    public SpecialtyDto updateSpecialty(Long id, SpecialtyDto dto) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con ID: " + id));

        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            String normalizedCode = dto.getCode().trim().toUpperCase();
            if (!specialty.getCode().equals(normalizedCode) && specialtyRepository.existsByCode(normalizedCode)) {
                throw new ConflictException("Ya existe otra especialidad registrada con el código: " + normalizedCode);
            }
            specialty.setCode(normalizedCode);
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            specialty.setName(dto.getName().trim());
        }

        if (dto.getDescription() != null) {
            specialty.setDescription(dto.getDescription().trim());
        }

        if (dto.getIcon() != null && !dto.getIcon().isBlank()) {
            specialty.setIcon(dto.getIcon().trim());
        }

        specialty.setActive(dto.isActive());
        specialty.setDisplayOrder(dto.getDisplayOrder());

        return toDto(specialtyRepository.save(specialty));
    }

    @Transactional
    public SpecialtyDto toggleActive(Long id, boolean active) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con ID: " + id));
        specialty.setActive(active);
        return toDto(specialtyRepository.save(specialty));
    }

    public SpecialtyDto toDto(Specialty entity) {
        if (entity == null) return null;
        SpecialtyDto dto = new SpecialtyDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setIcon(entity.getIcon());
        dto.setActive(entity.isActive());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
