package com.clinica.backend.service;

import com.clinica.backend.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CatalogAuthorizationService {

    public String resolveEffectiveSpecialty(String requestedSpecialty, Authentication authentication) {
        User user = authenticatedUser(authentication);
        String normalizedRequest = normalize(requestedSpecialty);

        if (hasSiteAdminRole(authentication)) {
            return normalizedRequest != null ? normalizedRequest : "ALL";
        }

        String userSpecialty = normalize(user.getSpecialty());
        if (userSpecialty == null) {
            userSpecialty = "GENERAL";
        }

        if (normalizedRequest != null && !normalizedRequest.equals(userSpecialty)) {
            throw new AccessDeniedException("Acceso fuera del ámbito autorizado");
        }
        return userSpecialty;
    }

    public boolean isCatalogAccessible(String catalogSpecialty, String effectiveSpecialty) {
        String normalizedCatalogSpecialty = normalize(catalogSpecialty);
        return "ALL".equals(effectiveSpecialty)
                || normalizedCatalogSpecialty == null
                || "GENERAL".equals(normalizedCatalogSpecialty)
                || normalizedCatalogSpecialty.equals(effectiveSpecialty);
    }

    public void ensureCatalogAccessible(String catalogSpecialty, String effectiveSpecialty) {
        if (!isCatalogAccessible(catalogSpecialty, effectiveSpecialty)) {
            throw new AccessDeniedException("Acceso fuera del ámbito autorizado");
        }
    }

    public void ensureTargetSpecialtyAccessible(String targetSpecialty, String effectiveSpecialty) {
        if ("ALL".equals(effectiveSpecialty)) {
            return;
        }
        String normalizedTarget = normalize(targetSpecialty);
        if (normalizedTarget != null
                && !"GENERAL".equals(normalizedTarget)
                && !normalizedTarget.equals(effectiveSpecialty)) {
            throw new AccessDeniedException("Acceso fuera del ámbito autorizado");
        }
    }

    private User authenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AccessDeniedException("Acceso no autorizado");
        }
        return user;
    }

    private boolean hasSiteAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SITE_ADMIN".equals(authority.getAuthority()));
    }

    private String normalize(String specialty) {
        if (specialty == null || specialty.isBlank()) {
            return null;
        }
        return specialty.trim().toUpperCase(Locale.ROOT);
    }
}
