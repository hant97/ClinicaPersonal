package com.clinica.backend.controller;

import com.clinica.backend.dto.CatalogDto;
import com.clinica.backend.dto.CatalogItemDto;
import com.clinica.backend.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import com.clinica.backend.model.User;

@RestController
@RequestMapping({"/api/v1/catalogs", "/api/catalogs"})
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<Page<CatalogDto>> getAllCatalogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        String specialty = getSpecialtyFromAuthentication(authentication);
        return ResponseEntity.ok(catalogService.getAllCatalogs(specialty, PageRequest.of(page, size)));
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogDto> createCatalog(@Valid @RequestBody CatalogDto dto) {
        return ResponseEntity.ok(catalogService.createCatalog(dto));
    }

    @PostMapping("/{code}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogItemDto> addCatalogItem(@PathVariable String code, @Valid @RequestBody CatalogItemDto itemDto) {
        return ResponseEntity.ok(catalogService.addCatalogItem(code, itemDto));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogItemDto> updateCatalogItem(@PathVariable Long itemId, @Valid @RequestBody CatalogItemDto itemDto) {
        return ResponseEntity.ok(catalogService.updateCatalogItem(itemId, itemDto));
    }

    private String getSpecialtyFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getSpecialty();
        }
        return "PSICOLOGIA"; // Default
    }
}
