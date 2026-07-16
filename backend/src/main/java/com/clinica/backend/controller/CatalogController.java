package com.clinica.backend.controller;

import com.clinica.backend.dto.CatalogDto;
import com.clinica.backend.dto.CatalogItemDto;
import com.clinica.backend.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/catalogs")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    @GetMapping
    public ResponseEntity<Page<CatalogDto>> getAllCatalogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(catalogService.getAllCatalogs(PageRequest.of(page, size)));
    }

    @GetMapping("/{code}")
    public ResponseEntity<CatalogDto> getCatalogByCode(@PathVariable String code) {
        return ResponseEntity.ok(catalogService.getCatalogByCode(code));
    }

    @GetMapping("/{code}/items/active")
    public ResponseEntity<List<CatalogItemDto>> getActiveItemsByCatalogCode(@PathVariable String code) {
        return ResponseEntity.ok(catalogService.getActiveItemsByCatalogCode(code));
    }

    @PostMapping
    public ResponseEntity<CatalogDto> createCatalog(@RequestBody CatalogDto dto) {
        return ResponseEntity.ok(catalogService.createCatalog(dto));
    }

    @PostMapping("/{code}/items")
    public ResponseEntity<CatalogItemDto> addCatalogItem(@PathVariable String code, @RequestBody CatalogItemDto itemDto) {
        return ResponseEntity.ok(catalogService.addCatalogItem(code, itemDto));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CatalogItemDto> updateCatalogItem(@PathVariable Long itemId, @RequestBody CatalogItemDto itemDto) {
        return ResponseEntity.ok(catalogService.updateCatalogItem(itemId, itemDto));
    }
}
