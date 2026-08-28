package com.clinica.backend.service;

import com.clinica.backend.repository.ClinicSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class FileStorageSecurityTest {

    @TempDir
    Path tempDir;

    private LocalWebsiteFileStorage websiteFileStorage;
    private ClinicSettingsService clinicSettingsService;
    private ClinicSettingsRepository clinicSettingsRepository;

    @BeforeEach
    void setUp() {
        websiteFileStorage = new LocalWebsiteFileStorage(tempDir.toString());
        clinicSettingsRepository = mock(ClinicSettingsRepository.class);
        clinicSettingsService = new ClinicSettingsService(clinicSettingsRepository, new com.clinica.backend.mapper.ClinicSettingsMapperImpl());
    }

    @Test
    void websiteFileStorageShouldRejectSvgFile() {
        MockMultipartFile svgFile = new MockMultipartFile(
                "file",
                "malicious.svg",
                "image/svg+xml",
                "<svg onload=\"alert(1)\"></svg>".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () ->
                websiteFileStorage.store(svgFile, "logo", null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                websiteFileStorage.storeDraft(svgFile, "logo")
        );
    }

    @Test
    void websiteFileStorageShouldRejectSvgWithPngExtension() {
        MockMultipartFile fakePngFile = new MockMultipartFile(
                "file",
                "fake.png",
                "image/svg+xml",
                "<svg onload=\"alert(1)\"></svg>".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () ->
                websiteFileStorage.store(fakePngFile, "logo", null)
        );
    }

    @Test
    void clinicSettingsServiceShouldRejectSvgLogo() {
        MockMultipartFile svgFile = new MockMultipartFile(
                "file",
                "logo.svg",
                "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () ->
                clinicSettingsService.uploadLogo(svgFile, "PSICOLOGIA")
        );
    }
}
