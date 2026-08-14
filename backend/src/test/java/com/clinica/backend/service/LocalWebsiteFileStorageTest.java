package com.clinica.backend.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalWebsiteFileStorageTest {

    private Path tempDir;
    private LocalWebsiteFileStorage storage;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("website-storage-test");
        storage = new LocalWebsiteFileStorage(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        }
    }

    @Test
    void rejectsDisallowedMimeType() {
        MultipartFile file = new MockMultipartFile("file", "photo.png", "text/plain", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class, () -> storage.storeDraft(file, "hero"));
    }

    @Test
    void rejectsDisallowedExtension() {
        MultipartFile file = new MockMultipartFile("file", "photo.exe", "image/png", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class, () -> storage.storeDraft(file, "hero"));
    }

    @Test
    void rejectsOversizedFile() {
        byte[] content = new byte[3 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", content);

        assertThrows(IllegalArgumentException.class, () -> storage.storeDraft(file, "hero"));
    }

    @Test
    void storesDraftUnderSeparateKeyAndPromotesToPublished() {
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        String draftKey = storage.storeDraft(file, "hero");

        assertTrue(draftKey.startsWith("draft/hero/"));
        assertTrue(storage.isDraftKey(draftKey));

        String publishedKey = storage.promote(draftKey);

        assertEquals("hero/" + draftKey.substring("draft/hero/".length()), publishedKey);
        assertTrue(Files.exists(tempDir.resolve(publishedKey)));
    }
}
