package com.clinica.backend.controller;

import com.clinica.backend.dto.CatalogDto;
import com.clinica.backend.dto.CatalogItemDto;
import com.clinica.backend.service.CatalogAuthorizationService;
import com.clinica.backend.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogs")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;
    private final CatalogAuthorizationService catalogAuthorizationService;

    @GetMapping
    public ResponseEntity<Page<CatalogDto>> getAllCatalogs(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String specialty,
            Authentication authentication) {
        String effectiveSpecialty = resolveSpecialty(specialty, authentication);
        return ResponseEntity.ok(catalogService.getAllCatalogs(effectiveSpecialty, search, pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CatalogDto>> getAllAccessibleCatalogs(
            @RequestParam(required = false) String specialty,
            Authentication authentication) {
        String effectiveSpecialty = resolveSpecialty(specialty, authentication);
        return ResponseEntity.ok(catalogService.getAllAccessibleCatalogs(effectiveSpecialty));
    }

    @GetMapping("/{code}")
    public ResponseEntity<CatalogDto> getCatalogByCode(@PathVariable String code, Authentication authentication) {
        return ResponseEntity.ok(catalogService.getCatalogByCode(code, resolveSpecialty(null, authentication)));
    }

    @GetMapping("/{code}/items/active")
    public ResponseEntity<List<CatalogItemDto>> getActiveItemsByCatalogCode(
            @PathVariable String code,
            Authentication authentication) {
        return ResponseEntity.ok(catalogService.getActiveItemsByCatalogCode(
                code,
                resolveSpecialty(null, authentication)
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<CatalogDto> createCatalog(
            @Valid @RequestBody CatalogDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(catalogService.createCatalog(dto, resolveSpecialty(null, authentication)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<CatalogDto> updateCatalog(
            @PathVariable Long id,
            @RequestBody CatalogDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(catalogService.updateCatalog(id, dto, resolveSpecialty(null, authentication)));
    }

    @PostMapping("/{code}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<CatalogItemDto> addCatalogItem(
            @PathVariable String code,
            @Valid @RequestBody CatalogItemDto itemDto,
            Authentication authentication) {
        return ResponseEntity.ok(catalogService.addCatalogItem(code, itemDto, resolveSpecialty(null, authentication)));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<CatalogItemDto> updateCatalogItem(
            @PathVariable Long itemId,
            @Valid @RequestBody CatalogItemDto itemDto,
            Authentication authentication) {
        return ResponseEntity.ok(catalogService.updateCatalogItem(
                itemId,
                itemDto,
                resolveSpecialty(null, authentication)
        ));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<Void> deleteCatalogItem(@PathVariable Long itemId, Authentication authentication) {
        catalogService.deleteCatalogItem(itemId, resolveSpecialty(null, authentication));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{code}/items/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<List<CatalogItemDto>> reorderCatalogItems(
            @PathVariable String code,
            @RequestBody List<Long> orderedItemIds,
            Authentication authentication) {
        return ResponseEntity.ok(catalogService.reorderCatalogItems(
                code,
                orderedItemIds,
                resolveSpecialty(null, authentication)
        ));
    }

    private String resolveSpecialty(String requestedSpecialty, Authentication authentication) {
        return catalogAuthorizationService.resolveEffectiveSpecialty(requestedSpecialty, authentication);
    }
}
