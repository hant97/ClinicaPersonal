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

    private BlobWebsiteFileStorage websiteFileStorage;
    private ClinicSettingsService clinicSettingsService;
    private ClinicSettingsRepository clinicSettingsRepository;

    @BeforeEach
    void setUp() {
        LocalBlobStore blobStore = new LocalBlobStore(tempDir.toString());
        websiteFileStorage = new BlobWebsiteFileStorage(blobStore);
        clinicSettingsRepository = mock(ClinicSettingsRepository.class);
        clinicSettingsService = new ClinicSettingsService(clinicSettingsRepository,
                new com.clinica.backend.mapper.ClinicSettingsMapperImpl(), new LogoFileStorage(blobStore));
    }

    @Test
    void clinicalFileStorageShouldRejectSvgDisguisedAsPngWithPngContentType() {
        ClinicalFileStorage clinicalFileStorage = new ClinicalFileStorage(new LocalBlobStore(tempDir.toString()));
        MockMultipartFile disguisedSvg = new MockMultipartFile(
                "file",
                "lesion.png",
                "image/png",
                "<svg onload=\"alert(1)\"></svg>".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () ->
                clinicalFileStorage.store(disguisedSvg, "lesions")
        );
    }

    @Test
    void logoShouldNotBeLoadedFromOutsideLogosFolder() {
        assertThrows(IllegalArgumentException.class, () -> clinicSettingsService.loadLogo("../website/hero.png"));
        assertThrows(IllegalArgumentException.class, () -> clinicSettingsService.loadLogo("sub/logo.png"));
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
