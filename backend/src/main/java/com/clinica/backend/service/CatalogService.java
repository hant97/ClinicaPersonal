package com.clinica.backend.service;

import com.clinica.backend.dto.CatalogDto;
import com.clinica.backend.dto.CatalogItemDto;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Catalog;
import com.clinica.backend.model.CatalogItem;
import com.clinica.backend.repository.CatalogItemRepository;
import com.clinica.backend.repository.CatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CatalogRepository catalogRepository;
    private final CatalogItemRepository catalogItemRepository;

    public Page<CatalogDto> getAllCatalogs(String specialty, Pageable pageable) {
        return getAllCatalogs(specialty, null, pageable);
    }

    public Page<CatalogDto> getAllCatalogs(String specialty, String search, Pageable pageable) {
        return catalogRepository.findAccessibleCatalogs(specialty, search, pageable)
                .map(this::mapToDto);
    }

    public List<CatalogDto> getAllAccessibleCatalogs(String specialty) {
        return catalogRepository.findAllAccessible(specialty)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public CatalogDto getCatalogByCode(String code) {
        Catalog catalog = catalogRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo no encontrado: " + code));
        return mapToDto(catalog);
    }

    @Cacheable("catalogItems")
    public List<CatalogItemDto> getActiveItemsByCatalogCode(String code) {
        return catalogItemRepository.findByCatalogCodeAndActiveTrueOrderByOrderIndexAsc(code)
                .stream()
                .map(this::mapItemToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CatalogDto createCatalog(CatalogDto dto) {
        if (catalogRepository.existsByCode(dto.getCode())) {
            throw new ConflictException("Ya existe un catálogo con el código: " + dto.getCode());
        }

        Catalog catalog = new Catalog();
        catalog.setCode(dto.getCode().trim().toUpperCase());
        catalog.setName(dto.getName().trim());
        catalog.setDescription(dto.getDescription());
        catalog.setSpecialty(dto.getSpecialty() != null && !dto.getSpecialty().isBlank() ? dto.getSpecialty() : "GENERAL");

        Catalog saved = catalogRepository.save(catalog);
        return mapToDto(saved);
    }

    @Transactional
    public CatalogDto updateCatalog(Long id, CatalogDto dto) {
        Catalog catalog = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo no encontrado con id: " + id));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            catalog.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) {
            catalog.setDescription(dto.getDescription().trim());
        }
        if (dto.getSpecialty() != null && !dto.getSpecialty().isBlank()) {
            catalog.setSpecialty(dto.getSpecialty());
        }

        Catalog saved = catalogRepository.save(catalog);
        return mapToDto(saved);
    }

    @Transactional
    @CacheEvict(value = "catalogItems", allEntries = true)
    public CatalogItemDto addCatalogItem(String catalogCode, CatalogItemDto itemDto) {
        Catalog catalog = catalogRepository.findByCode(catalogCode)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo no encontrado: " + catalogCode));

        String sanitizedItemCode = itemDto.getItemCode() != null && !itemDto.getItemCode().isBlank()
                ? itemDto.getItemCode().trim().toUpperCase()
                : generateItemCode(itemDto.getItemName());

        boolean exists = catalog.getItems().stream()
                .anyMatch(i -> i.getItemCode().equalsIgnoreCase(sanitizedItemCode));
        if (exists) {
            throw new ConflictException("Ya existe una opción con el código '" + sanitizedItemCode + "' en este catálogo");
        }

        CatalogItem item = new CatalogItem();
        item.setCatalog(catalog);
        item.setItemCode(sanitizedItemCode);
        item.setItemName(itemDto.getItemName().trim());
        item.setActive(itemDto.isActive());
        
        if (itemDto.getOrderIndex() != null) {
            item.setOrderIndex(itemDto.getOrderIndex());
        } else {
            int nextIndex = catalog.getItems().stream()
                    .mapToInt(i -> i.getOrderIndex() != null ? i.getOrderIndex() : 0)
                    .max()
                    .orElse(-1) + 1;
            item.setOrderIndex(nextIndex);
        }

        CatalogItem saved = catalogItemRepository.save(item);
        return mapItemToDto(saved);
    }

    @Transactional
    @CacheEvict(value = "catalogItems", allEntries = true)
    public CatalogItemDto updateCatalogItem(Long itemId, CatalogItemDto itemDto) {
        CatalogItem item = catalogItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de catálogo no encontrado: " + itemId));

        if (itemDto.getItemName() != null && !itemDto.getItemName().isBlank()) {
            item.setItemName(itemDto.getItemName().trim());
        }
        item.setActive(itemDto.isActive());
        if (itemDto.getOrderIndex() != null) {
            item.setOrderIndex(itemDto.getOrderIndex());
        }

        CatalogItem saved = catalogItemRepository.save(item);
        return mapItemToDto(saved);
    }

    @Transactional
    @CacheEvict(value = "catalogItems", allEntries = true)
    public void deleteCatalogItem(Long itemId) {
        CatalogItem item = catalogItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem de catálogo no encontrado con id: " + itemId));
        catalogItemRepository.delete(item);
    }

    @Transactional
    @CacheEvict(value = "catalogItems", allEntries = true)
    public List<CatalogItemDto> reorderCatalogItems(String catalogCode, List<Long> orderedItemIds) {
        catalogRepository.findByCode(catalogCode)
                .orElseThrow(() -> new ResourceNotFoundException("Catálogo no encontrado: " + catalogCode));

        List<CatalogItem> items = catalogItemRepository.findByCatalogCodeOrderByOrderIndexAsc(catalogCode);
        for (int i = 0; i < orderedItemIds.size(); i++) {
            Long itemId = orderedItemIds.get(i);
            int newOrder = i;
            items.stream()
                    .filter(item -> item.getId().equals(itemId))
                    .findFirst()
                    .ifPresent(item -> item.setOrderIndex(newOrder));
        }

        List<CatalogItem> saved = catalogItemRepository.saveAll(items);
        return saved.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getOrderIndex() != null ? a.getOrderIndex() : 0,
                        b.getOrderIndex() != null ? b.getOrderIndex() : 0))
                .map(this::mapItemToDto)
                .collect(Collectors.toList());
    }

    private String generateItemCode(String name) {
        if (name == null || name.isBlank()) return "ITEM_" + System.currentTimeMillis();
        return name.trim().toUpperCase()
                .replaceAll("[ÁÀÄÂ]", "A")
                .replaceAll("[ÉÈËÊ]", "E")
                .replaceAll("[ÍÌÏÎ]", "I")
                .replaceAll("[ÓÒÖÔ]", "O")
                .replaceAll("[ÚÙÜÛ]", "U")
                .replaceAll("[Ñ]", "N")
                .replaceAll("[^A-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private CatalogDto mapToDto(Catalog catalog) {
        CatalogDto dto = new CatalogDto();
        dto.setId(catalog.getId());
        dto.setCode(catalog.getCode());
        dto.setName(catalog.getName());
        dto.setDescription(catalog.getDescription());
        dto.setSpecialty(catalog.getSpecialty());
        if (catalog.getItems() != null) {
            dto.setItems(catalog.getItems().stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getOrderIndex() != null ? a.getOrderIndex() : 0,
                            b.getOrderIndex() != null ? b.getOrderIndex() : 0))
                    .map(this::mapItemToDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private CatalogItemDto mapItemToDto(CatalogItem item) {
        CatalogItemDto dto = new CatalogItemDto();
        dto.setId(item.getId());
        dto.setCatalogId(item.getCatalog().getId());
        dto.setItemCode(item.getItemCode());
        dto.setItemName(item.getItemName());
        dto.setActive(item.isActive());
        dto.setOrderIndex(item.getOrderIndex());
        return dto;
    }
}
