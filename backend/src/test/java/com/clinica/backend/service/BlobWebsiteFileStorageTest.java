package com.clinica.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlobWebsiteFileStorageTest {

    @TempDir
    Path tempDir;

    private BlobWebsiteFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new BlobWebsiteFileStorage(new LocalBlobStore(tempDir.toString()));
    }

    @Test
    void rejectsDisallowedMimeType() {
        MultipartFile file = new MockMultipartFile("file", "photo.png", "text/plain", TestUploads.PNG);

        assertThrows(IllegalArgumentException.class, () -> storage.storeDraft(file, "hero"));
    }

    @Test
    void rejectsDisallowedExtension() {
        MultipartFile file = new MockMultipartFile("file", "photo.exe", "image/png", TestUploads.PNG);

        assertThrows(IllegalArgumentException.class, () -> storage.storeDraft(file, "hero"));
    }

    @Test
    void rejectsOversizedFile() {
        byte[] content = new byte[3 * 1024 * 1024];
        System.arraycopy(TestUploads.PNG, 0, content, 0, TestUploads.PNG.length);
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", content);

        assertThrows(IllegalArgumentException.class, () -> storage.storeDraft(file, "hero"));
    }

    @Test
    void rejectsContentThatDoesNotMatchDeclaredType() {
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "<html>".getBytes());

        assertThrows(IllegalArgumentException.class, () -> storage.storeDraft(file, "hero"));
    }

    @Test
    void storesDraftUnderSeparateKeyAndPromotesToPublished() {
        String draftKey = storage.storeDraft(TestUploads.png("photo.png"), "hero");

        assertTrue(draftKey.startsWith("draft/hero/"));
        assertTrue(storage.isDraftKey(draftKey));

        String publishedKey = storage.promote(draftKey);

        assertEquals("hero/" + draftKey.substring("draft/hero/".length()), publishedKey);
        assertTrue(Files.exists(tempDir.resolve("website").resolve(publishedKey)));
    }

    @Test
    void replacingAnImageDeletesThePreviousOne() {
        String first = storage.store(TestUploads.png("logo.png"), "logo", null);
        String second = storage.store(TestUploads.png("logo.png"), "logo", first);

        assertFalse(Files.exists(tempDir.resolve("website").resolve(first)));
        assertTrue(Files.exists(tempDir.resolve("website").resolve(second)));
    }

    @Test
    void buildsPublicUrlOnlyForValidKeys() {
        assertEquals("/api/v1/public/website-assets/hero/a.png", storage.publicUrl("hero/a.png"));
        assertThrows(IllegalArgumentException.class, () -> storage.publicUrl("../secret.png"));
    }
}
