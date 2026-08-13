package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalServiceDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.repository.ClinicalServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import com.clinica.backend.model.User;

@Service
@RequiredArgsConstructor
public class ClinicalServiceService {

    private final ClinicalServiceRepository clinicalServiceRepository;

    private String getCurrentUserSpecialty() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSpecialty();
    }

    public Page<ClinicalServiceDto> getAllServices(String name, Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        if (name != null && !name.trim().isEmpty()) {
            return clinicalServiceRepository.findBySpecialtyAndNameContainingIgnoreCaseAndDeletedFalse(specialty, name, pageable).map(this::mapToDto);
        }
        return clinicalServiceRepository.findBySpecialtyAndDeletedFalse(specialty, pageable).map(this::mapToDto);
    }

    public List<ClinicalServiceDto> getAllActiveServices() {
        String specialty = getCurrentUserSpecialty();
        return clinicalServiceRepository.findBySpecialtyAndDeletedFalse(specialty).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ClinicalServiceDto getServiceById(Long id) {
        ClinicalService service = clinicalServiceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio clínico no encontrado"));
        return mapToDto(service);
    }

    public ClinicalServiceDto createService(ClinicalServiceDto dto) {
        ClinicalService service = new ClinicalService();
        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setPrice(dto.getPrice());
        service.setSpecialty(getCurrentUserSpecialty());
        ClinicalService saved = clinicalServiceRepository.save(service);
        return mapToDto(saved);
    }

    public ClinicalServiceDto updateService(Long id, ClinicalServiceDto dto) {
        ClinicalService service = clinicalServiceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio clínico no encontrado"));
        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setPrice(dto.getPrice());
        ClinicalService updated = clinicalServiceRepository.save(service);
        return mapToDto(updated);
    }

    public void deleteService(Long id) {
        ClinicalService service = clinicalServiceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio clínico no encontrado"));
        service.setDeleted(true);
        clinicalServiceRepository.save(service);
    }

    private ClinicalServiceDto mapToDto(ClinicalService service) {
        ClinicalServiceDto dto = new ClinicalServiceDto();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setPrice(service.getPrice());
        dto.setSpecialty(service.getSpecialty());
        return dto;
    }
}
