package com.clinica.backend.service;

import com.clinica.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogAuthorizationServiceTest {

    private final CatalogAuthorizationService service = new CatalogAuthorizationService();

    @Test
    void professionalUsesAuthenticatedSpecialtyWhenParameterIsOmitted() {
        Authentication authentication = authentication("PSICOLOGIA", "ROLE_PROFESSIONAL");

        assertEquals("PSICOLOGIA", service.resolveEffectiveSpecialty(null, authentication));
        assertDoesNotThrow(() -> service.ensureCatalogAccessible("GENERAL", "PSICOLOGIA"));
        assertDoesNotThrow(() -> service.ensureCatalogAccessible("PSICOLOGIA", "PSICOLOGIA"));
    }

    @Test
    void clinicalAdministratorCannotRequestAllOrAnotherSpecialty() {
        Authentication authentication = authentication("DERMATOLOGIA", "ROLE_ADMIN");

        assertThrows(AccessDeniedException.class,
                () -> service.resolveEffectiveSpecialty("ALL", authentication));
        assertThrows(AccessDeniedException.class,
                () -> service.resolveEffectiveSpecialty("PSICOLOGIA", authentication));
    }

    @Test
    void siteAdministratorCanRequestAllOrAnExplicitSpecialty() {
        Authentication authentication = authentication("GENERAL", "ROLE_SITE_ADMIN");

        assertEquals("ALL", service.resolveEffectiveSpecialty("ALL", authentication));
        assertEquals("PSICOLOGIA", service.resolveEffectiveSpecialty("psicologia", authentication));
        assertEquals("ALL", service.resolveEffectiveSpecialty(null, authentication));
    }

    @Test
    void crossSpecialtyCatalogIsRejected() {
        assertThrows(AccessDeniedException.class,
                () -> service.ensureCatalogAccessible("DERMATOLOGIA", "PSICOLOGIA"));
    }

    private Authentication authentication(String specialty, String... roles) {
        User user = new User();
        user.setUsername("test-user");
        user.setPassword("unused");
        user.setSpecialty(specialty);
        user.setEnabled(true);
        user.setRoles(Set.of(roles));
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
