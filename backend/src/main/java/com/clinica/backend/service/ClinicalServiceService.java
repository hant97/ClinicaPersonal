package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalServiceDto;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.repository.ClinicalServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClinicalServiceService {

    private final ClinicalServiceRepository clinicalServiceRepository;

    public Page<ClinicalServiceDto> getAllServices(String name, Pageable pageable) {
        if (name != null && !name.trim().isEmpty()) {
            return clinicalServiceRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name, pageable).map(this::mapToDto);
        }
        return clinicalServiceRepository.findByDeletedFalse(pageable).map(this::mapToDto);
    }

    public List<ClinicalServiceDto> getAllActiveServices() {
        return clinicalServiceRepository.findByDeletedFalse().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ClinicalServiceDto getServiceById(Long id) {
        ClinicalService service = clinicalServiceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id " + id));
        return mapToDto(service);
    }

    public ClinicalServiceDto createService(ClinicalServiceDto dto) {
        ClinicalService service = new ClinicalService();
        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setPrice(dto.getPrice());
        ClinicalService saved = clinicalServiceRepository.save(service);
        return mapToDto(saved);
    }

    public ClinicalServiceDto updateService(Long id, ClinicalServiceDto dto) {
        ClinicalService service = clinicalServiceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id " + id));
        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setPrice(dto.getPrice());
        ClinicalService updated = clinicalServiceRepository.save(service);
        return mapToDto(updated);
    }

    public void deleteService(Long id) {
        ClinicalService service = clinicalServiceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id " + id));
        service.setDeleted(true);
        clinicalServiceRepository.save(service);
    }

    private ClinicalServiceDto mapToDto(ClinicalService service) {
        ClinicalServiceDto dto = new ClinicalServiceDto();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setPrice(service.getPrice());
        return dto;
    }
}
