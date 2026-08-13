package com.clinica.backend.controller;

import com.clinica.backend.service.WebsiteSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicLandingControllerTest {

    @Test
    void assetPathCaptureDoesNotPassLeadingSlashToStorage() {
        WebsiteSettingsService service = mock(WebsiteSettingsService.class);
        Resource resource = new ByteArrayResource(new byte[0]);
        when(service.loadAsset("hero/image.png")).thenReturn(resource);

        PublicLandingController controller = new PublicLandingController(service);

        assertSame(resource, controller.getAsset("/hero/image.png").getBody());
        verify(service).loadAsset("hero/image.png");
    }
}
