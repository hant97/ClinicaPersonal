package com.clinica.backend.service;

import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.dto.SupplyDto;
import com.clinica.backend.dto.SupplyStatsDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Supply;
import com.clinica.backend.model.TransactionReason;
import com.clinica.backend.model.TransactionType;
import com.clinica.backend.repository.SupplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import com.clinica.backend.model.User;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SupplyService {

    private final SupplyRepository supplyRepository;
    private final WebsiteFileStorage fileStorage;
    private final InventoryTransactionService inventoryTransactionService;

    private String getCurrentUserSpecialty() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSpecialty();
    }

    @Transactional(readOnly = true)
    public Page<SupplyDto> getAllSupplies(String name, Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        if (name != null && !name.trim().isEmpty()) {
            return supplyRepository.findBySpecialtyAndNameContainingIgnoreCaseAndDeletedFalse(specialty, name, pageable).map(this::mapToDto);
        }
        return supplyRepository.findBySpecialtyAndDeletedFalse(specialty, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public SupplyDto getSupplyById(Long id) {
        String specialty = getCurrentUserSpecialty();
        Supply supply = supplyRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Suministro", "id", id));
        return mapToDto(supply);
    }
    
    @Transactional(readOnly = true)
    public List<SupplyDto> getLowStockSupplies() {
        String specialty = getCurrentUserSpecialty();
        return supplyRepository.findLowStockSuppliesBySpecialty(specialty).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplyStatsDto getStats() {
        String specialty = getCurrentUserSpecialty();
        LocalDate today = LocalDate.now();
        long totalSupplies = supplyRepository.countBySpecialtyAndDeletedFalse(specialty);
        long lowStockCount = supplyRepository.countLowStockBySpecialty(specialty);
        long outOfStockCount = supplyRepository.countOutOfStockBySpecialty(specialty);
        long expiringSoonCount = supplyRepository.countExpiringBetweenBySpecialty(today, today.plusDays(30), specialty);
        BigDecimal inventoryValue = supplyRepository.sumInventoryValueBySpecialty(specialty);

        return SupplyStatsDto.builder()
                .totalSupplies(totalSupplies)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .expiringSoonCount(expiringSoonCount)
                .inventoryValue(inventoryValue != null ? inventoryValue : BigDecimal.ZERO)
                .build();
    }

    @Transactional
    public SupplyDto createSupply(SupplyDto supplyDto) {
        Supply supply = mapToEntity(supplyDto);
        supply.setSpecialty(getCurrentUserSpecialty());
        Supply savedSupply = supplyRepository.save(supply);
        return mapToDto(savedSupply);
    }

    @Transactional
    public SupplyDto updateSupply(Long id, SupplyDto supplyDto) {
        String specialty = getCurrentUserSpecialty();
        Supply supply = supplyRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Suministro", "id", id));

        int oldStock = supply.getCurrentStock() != null ? supply.getCurrentStock() : 0;
        int newStock = supplyDto.getCurrentStock() != null ? supplyDto.getCurrentStock() : 0;
        int stockDiff = newStock - oldStock;

        supply.setName(supplyDto.getName());
        supply.setDescription(supplyDto.getDescription());
        supply.setMinStockLevel(supplyDto.getMinStockLevel());
        supply.setUnit(supplyDto.getUnit());
        supply.setPrice(supplyDto.getPrice());
        supply.setExpirationDate(supplyDto.getExpirationDate());
        supply.setImageUrl(trimToNull(supplyDto.getImageUrl()));

        Supply updatedSupply = supplyRepository.save(supply);

        if (stockDiff != 0) {
            InventoryTransactionDto txDto = new InventoryTransactionDto();
            txDto.setSupplyId(updatedSupply.getId());
            txDto.setQuantity(Math.abs(stockDiff));
            txDto.setType(stockDiff > 0 ? TransactionType.IN : TransactionType.OUT);
            txDto.setReason(stockDiff > 0 ? TransactionReason.RESTOCK : TransactionReason.DAMAGED);
            txDto.setNotes("Ajuste manual de stock en edición de insumo");
            inventoryTransactionService.recordTransaction(txDto);
            updatedSupply.setCurrentStock(newStock);
        }

        return mapToDto(updatedSupply);
    }

    @Transactional
    public void deleteSupply(Long id) {
        String specialty = getCurrentUserSpecialty();
        Supply supply = supplyRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Suministro", "id", id));
        supply.setDeleted(true);
        supplyRepository.save(supply);
    }

    @Transactional
    public SupplyDto uploadImage(Long id, MultipartFile file) {
        String specialty = getCurrentUserSpecialty();
        Supply supply = supplyRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Suministro", "id", id));
        String previousKey = toAssetKey(supply.getImageUrl());
        String key = fileStorage.store(file, "supplies", previousKey);
        supply.setImageUrl(fileStorage.publicUrl(key));
        supplyRepository.save(supply);
        return mapToDto(supply);
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
        dto.setSpecialty(supply.getSpecialty());
        dto.setImageUrl(supply.getImageUrl());
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
        supply.setImageUrl(trimToNull(dto.getImageUrl()));
        if (dto.getSpecialty() != null) {
            supply.setSpecialty(dto.getSpecialty());
        }
        return supply;
    }

    private String toAssetKey(String imageUrl) {
        String prefix = "/api/v1/public/website-assets/";
        if (imageUrl == null || !imageUrl.startsWith(prefix)) {
            return null;
        }
        return imageUrl.substring(prefix.length());
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
