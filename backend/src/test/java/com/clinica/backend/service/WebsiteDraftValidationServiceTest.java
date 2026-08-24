package com.clinica.backend.service;

import com.clinica.backend.dto.WebsiteDraftDto;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebsiteDraftValidationServiceTest {

    private Validator validator;
    private WebsiteDraftValidationService service;
    private WebsiteDraftDto draft;

    @BeforeEach
    void setUp() {
        validator = mock(Validator.class);
        when(validator.validate(any(WebsiteDraftDto.class))).thenReturn(Collections.emptySet());
        service = new WebsiteDraftValidationService(validator);
        draft = new WebsiteDraftDto();
    }

    @Test
    void acceptsHttpAndHttpsUrls() {
        draft.setFacebookUrl("https://example.com/clinica");
        draft.setMapUrl("http://maps.example.test/location");

        assertDoesNotThrow(() -> service.validate(draft));
    }

    @Test
    void rejectsRelativeOrUnsupportedUrls() {
        draft.setFacebookUrl("javascript:alert(1)");

        assertThrows(IllegalArgumentException.class, () -> service.validate(draft));
    }

    @Test
    void rejectsIconsOutsideTheAllowList() {
        WebsiteDraftDto.Benefit benefit = new WebsiteDraftDto.Benefit();
        benefit.setIconCode("UNSAFE_ICON");
        draft.getBenefits().add(benefit);

        assertThrows(IllegalArgumentException.class, () -> service.validate(draft));
    }
}
