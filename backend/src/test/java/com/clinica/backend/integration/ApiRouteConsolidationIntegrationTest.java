package com.clinica.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.core.env.Environment;

import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ApiRouteConsolidationIntegrationTest {

    private static final Set<String> REMOVED_LEGACY_PREFIXES = Set.of(
            "/api/assessments",
            "/api/catalogs",
            "/api/clinical-sessions",
            "/api/settings/clinic",
            "/api/alerts",
            "/api/inventory-transactions",
            "/api/tests",
            "/api/specialties",
            "/api/supplies",
            "/api/patients/{patientId}/alerts"
    );

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private Environment environment;

    @Test
    void applicationApiRoutesAreVersionedAndLegacyAliasesAreAbsent() {
        Set<String> apiPatterns = new TreeSet<>();
        handlerMapping.getHandlerMethods().keySet().forEach(info ->
                info.getPatternValues().stream()
                        .filter(pattern -> pattern.startsWith("/api/"))
                        .forEach(apiPatterns::add));

        assertThat(apiPatterns)
                .isNotEmpty()
                .allMatch(pattern -> pattern.startsWith("/api/v1/"));
        assertThat(apiPatterns)
                .noneMatch(pattern -> REMOVED_LEGACY_PREFIXES.stream().anyMatch(pattern::startsWith));
    }

    @Test
    void openSessionInViewIsDisabled() {
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isFalse();
    }
}
