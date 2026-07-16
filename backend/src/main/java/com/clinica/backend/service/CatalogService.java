package com.clinica.backend.service;

import com.clinica.backend.dto.CatalogDto;
import com.clinica.backend.dto.CatalogItemDto;
import com.clinica.backend.model.Catalog;
import com.clinica.backend.model.CatalogItem;
import com.clinica.backend.repository.CatalogItemRepository;
import com.clinica.backend.repository.CatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.Cacheable;

@Service
public class CatalogService {

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private CatalogItemRepository catalogItemRepository;

    public Page<CatalogDto> getAllCatalogs(Pageable pageable) {
        return catalogRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    public CatalogDto getCatalogByCode(String code) {
        Catalog catalog = catalogRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Catalog not found"));
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
        Catalog catalog = new Catalog();
        catalog.setCode(dto.getCode());
        catalog.setName(dto.getName());
        catalog.setDescription(dto.getDescription());
        
        Catalog saved = catalogRepository.save(catalog);
        return mapToDto(saved);
    }

    @Transactional
    public CatalogItemDto addCatalogItem(String catalogCode, CatalogItemDto itemDto) {
        Catalog catalog = catalogRepository.findByCode(catalogCode)
                .orElseThrow(() -> new RuntimeException("Catalog not found"));

        CatalogItem item = new CatalogItem();
        item.setCatalog(catalog);
        item.setItemCode(itemDto.getItemCode());
        item.setItemName(itemDto.getItemName());
        item.setActive(itemDto.isActive());
        item.setOrderIndex(itemDto.getOrderIndex() != null ? itemDto.getOrderIndex() : 0);

        CatalogItem saved = catalogItemRepository.save(item);
        return mapItemToDto(saved);
    }

    @Transactional
    public CatalogItemDto updateCatalogItem(Long itemId, CatalogItemDto itemDto) {
        CatalogItem item = catalogItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Catalog item not found"));

        item.setItemName(itemDto.getItemName());
        item.setActive(itemDto.isActive());
        if (itemDto.getOrderIndex() != null) {
            item.setOrderIndex(itemDto.getOrderIndex());
        }

        CatalogItem saved = catalogItemRepository.save(item);
        return mapItemToDto(saved);
    }

    private CatalogDto mapToDto(Catalog catalog) {
        CatalogDto dto = new CatalogDto();
        dto.setId(catalog.getId());
        dto.setCode(catalog.getCode());
        dto.setName(catalog.getName());
        dto.setDescription(catalog.getDescription());
        if (catalog.getItems() != null) {
            dto.setItems(catalog.getItems().stream()
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
