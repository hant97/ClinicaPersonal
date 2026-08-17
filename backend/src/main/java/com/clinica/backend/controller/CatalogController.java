package com.clinica.backend.controller;

import com.clinica.backend.dto.CatalogDto;
import com.clinica.backend.dto.CatalogItemDto;
import com.clinica.backend.model.User;
import com.clinica.backend.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/catalogs", "/api/catalogs"})
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<Page<CatalogDto>> getAllCatalogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String specialty,
            Authentication authentication) {
        String effectiveSpecialty = resolveSpecialty(specialty, authentication);
        return ResponseEntity.ok(catalogService.getAllCatalogs(effectiveSpecialty, search, PageRequest.of(page, size)));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CatalogDto>> getAllAccessibleCatalogs(
            @RequestParam(required = false) String specialty,
            Authentication authentication) {
        String effectiveSpecialty = resolveSpecialty(specialty, authentication);
        return ResponseEntity.ok(catalogService.getAllAccessibleCatalogs(effectiveSpecialty));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<CatalogDto> createCatalog(@Valid @RequestBody CatalogDto dto) {
        return ResponseEntity.ok(catalogService.createCatalog(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<CatalogDto> updateCatalog(@PathVariable Long id, @RequestBody CatalogDto dto) {
        return ResponseEntity.ok(catalogService.updateCatalog(id, dto));
    }

    @PostMapping("/{code}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<CatalogItemDto> addCatalogItem(@PathVariable String code, @Valid @RequestBody CatalogItemDto itemDto) {
        return ResponseEntity.ok(catalogService.addCatalogItem(code, itemDto));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<CatalogItemDto> updateCatalogItem(@PathVariable Long itemId, @Valid @RequestBody CatalogItemDto itemDto) {
        return ResponseEntity.ok(catalogService.updateCatalogItem(itemId, itemDto));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<Void> deleteCatalogItem(@PathVariable Long itemId) {
        catalogService.deleteCatalogItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{code}/items/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'SITE_ADMIN')")
    public ResponseEntity<List<CatalogItemDto>> reorderCatalogItems(
            @PathVariable String code,
            @RequestBody List<Long> orderedItemIds) {
        return ResponseEntity.ok(catalogService.reorderCatalogItems(code, orderedItemIds));
    }

    private String resolveSpecialty(String requestedSpecialty, Authentication authentication) {
        if (requestedSpecialty != null && !requestedSpecialty.isBlank()) {
            return requestedSpecialty;
        }
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            boolean isSiteAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SITE_ADMIN"));
            if (isSiteAdmin) {
                return "ALL";
            }
            return user.getSpecialty() != null ? user.getSpecialty() : "GENERAL";
        }
        return "GENERAL";
    }
}
