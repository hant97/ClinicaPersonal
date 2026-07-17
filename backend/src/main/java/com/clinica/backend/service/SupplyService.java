package com.clinica.backend.service;

import com.clinica.backend.dto.SupplyDto;
import com.clinica.backend.model.Supply;
import com.clinica.backend.repository.SupplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class SupplyService {

    private final SupplyRepository supplyRepository;

    public Page<SupplyDto> getAllSupplies(String name, Pageable pageable) {
        if (name != null && !name.trim().isEmpty()) {
            return supplyRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name, pageable).map(this::mapToDto);
        }
        return supplyRepository.findByDeletedFalse(pageable).map(this::mapToDto);
    }

    public SupplyDto getSupplyById(Long id) {
        Supply supply = supplyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Supply not found with id " + id));
        return mapToDto(supply);
    }
    
    public List<SupplyDto> getLowStockSupplies() {
        // Obtenemos todos y filtramos en memoria, o podemos usar una query si el umbral es dinámico
        return supplyRepository.findByDeletedFalse().stream()
                .filter(s -> s.getCurrentStock() != null && s.getMinStockLevel() != null && s.getCurrentStock() <= s.getMinStockLevel())
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public SupplyDto createSupply(SupplyDto supplyDto) {
        Supply supply = mapToEntity(supplyDto);
        Supply savedSupply = supplyRepository.save(supply);
        return mapToDto(savedSupply);
    }

    public SupplyDto updateSupply(Long id, SupplyDto supplyDto) {
        Supply supply = supplyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Supply not found with id " + id));

        supply.setName(supplyDto.getName());
        supply.setDescription(supplyDto.getDescription());
        supply.setCurrentStock(supplyDto.getCurrentStock());
        supply.setMinStockLevel(supplyDto.getMinStockLevel());
        supply.setUnit(supplyDto.getUnit());
        supply.setPrice(supplyDto.getPrice());
        supply.setExpirationDate(supplyDto.getExpirationDate());

        Supply updatedSupply = supplyRepository.save(supply);
        return mapToDto(updatedSupply);
    }

    public void deleteSupply(Long id) {
        Supply supply = supplyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Supply not found with id " + id));
        supply.setDeleted(true);
        supplyRepository.save(supply);
    }

    private SupplyDto mapToDto(Supply supply) {
        SupplyDto dto = new SupplyDto();
        dto.setId(supply.getId());
        dto.setName(supply.getName());
        dto.setDescription(supply.getDescription());
        dto.setCurrentStock(supply.getCurrentStock());
        dto.setMinStockLevel(supply.getMinStockLevel());
        dto.setUnit(supply.getUnit());
        dto.setPrice(supply.getPrice());
        dto.setExpirationDate(supply.getExpirationDate());
        return dto;
    }

    private Supply mapToEntity(SupplyDto dto) {
        Supply supply = new Supply();
        supply.setId(dto.getId());
        supply.setName(dto.getName());
        supply.setDescription(dto.getDescription());
        supply.setCurrentStock(dto.getCurrentStock());
        supply.setMinStockLevel(dto.getMinStockLevel());
        supply.setUnit(dto.getUnit());
        supply.setPrice(dto.getPrice());
        supply.setExpirationDate(dto.getExpirationDate());
        return supply;
    }
}
