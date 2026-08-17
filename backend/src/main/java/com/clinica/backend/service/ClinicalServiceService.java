package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalServiceDto;
import com.clinica.backend.dto.ClinicalServiceStatsDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ClinicalServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class ClinicalServiceService {

    private final ClinicalServiceRepository clinicalServiceRepository;

    private String getCurrentUserSpecialty() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSpecialty();
    }

    public Page<ClinicalServiceDto> getAllServices(String name, String category, Boolean active,
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        return clinicalServiceRepository
                .findAllWithFilters(name, category, active, minPrice, maxPrice, specialty, pageable)
                .map(this::mapToDto);
    }

    public List<ClinicalServiceDto> getAllActiveServices() {
        String specialty = getCurrentUserSpecialty();
        return clinicalServiceRepository.findBySpecialtyAndActiveTrueAndDeletedFalse(specialty)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClinicalServiceStatsDto getStats() {
        String specialty = getCurrentUserSpecialty();
        long totalServices = clinicalServiceRepository.countBySpecialtyAndDeletedFalse(specialty);
        long activeCount = clinicalServiceRepository.countActiveBySpecialty(specialty);
        BigDecimal averagePrice = clinicalServiceRepository.averagePriceBySpecialty(specialty);

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime startOfNextMonth = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Object[]> rows = clinicalServiceRepository.findTopServicesByRevenueWithIdBySpecialty(startOfMonth, startOfNextMonth, specialty, PageRequest.of(0, 5));
        List<ClinicalServiceStatsDto.ServicePerformance> topByRevenue = toPerformanceList(rows);

        List<ClinicalServiceStatsDto.ServicePerformance> topByQuantity = toPerformanceList(
                clinicalServiceRepository.findTopServicesByQuantityWithIdBySpecialty(startOfMonth, startOfNextMonth, specialty, PageRequest.of(0, 5)));

        return ClinicalServiceStatsDto.builder()
                .totalServices(totalServices)
                .activeCount(activeCount)
                .averagePrice(averagePrice != null ? averagePrice : BigDecimal.ZERO)
                .topByRevenue(topByRevenue)
                .topByQuantity(topByQuantity)
                .build();
    }

    private List<ClinicalServiceStatsDto.ServicePerformance> toPerformanceList(List<Object[]> rows) {
        return rows.stream()
                .map(row -> ClinicalServiceStatsDto.ServicePerformance.builder()
                        .serviceId(((Number) row[0]).longValue())
                        .name(row[1].toString())
                        .quantity(((Number) row[2]).longValue())
                        .total((BigDecimal) row[3])
                        .build())
                .collect(Collectors.toList());
    }

    public ClinicalServiceDto getServiceById(Long id) {
        String specialty = getCurrentUserSpecialty();
        ClinicalService service = clinicalServiceRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio clínico no encontrado"));
        return mapToDto(service);
    }

    public ClinicalServiceDto createService(ClinicalServiceDto dto) {
        ClinicalService service = new ClinicalService();
        applyDtoToEntity(service, dto);
        service.setSpecialty(getCurrentUserSpecialty());
        ClinicalService saved = clinicalServiceRepository.save(service);
        return mapToDto(saved);
    }

    public ClinicalServiceDto updateService(Long id, ClinicalServiceDto dto) {
        String specialty = getCurrentUserSpecialty();
        ClinicalService service = clinicalServiceRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio clínico no encontrado"));
        applyDtoToEntity(service, dto);
        ClinicalService updated = clinicalServiceRepository.save(service);
        return mapToDto(updated);
    }

    public void deleteService(Long id) {
        String specialty = getCurrentUserSpecialty();
        ClinicalService service = clinicalServiceRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio clínico no encontrado"));
        service.setDeleted(true);
        clinicalServiceRepository.save(service);
    }

    private void applyDtoToEntity(ClinicalService service, ClinicalServiceDto dto) {
        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setPrice(dto.getPrice());
        service.setCategory(dto.getCategory());
        service.setDurationMinutes(dto.getDurationMinutes());
        service.setImageUrl(dto.getImageUrl());
        service.setActive(dto.getActive() == null || dto.getActive());
    }

    private ClinicalServiceDto mapToDto(ClinicalService service) {
        ClinicalServiceDto dto = new ClinicalServiceDto();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setPrice(service.getPrice());
        dto.setCategory(service.getCategory());
        dto.setDurationMinutes(service.getDurationMinutes());
        dto.setImageUrl(service.getImageUrl());
        dto.setActive(service.isActive());
        dto.setSpecialty(service.getSpecialty());
        return dto;
    }
}
