package com.clinica.backend.service;

import com.clinica.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalBlobStoreTest {

    @TempDir
    Path tempDir;

    private LocalBlobStore store;

    @BeforeEach
    void setUp() {
        store = new LocalBlobStore(tempDir.toString());
    }

    @Test
    void putGetCopyAndDeleteRoundTrip() throws Exception {
        byte[] content = {1, 2, 3};
        store.put("clinical/lesions/a.png", new ByteArrayInputStream(content), content.length, "image/png");

        Resource resource = store.get("clinical/lesions/a.png");
        assertEquals("a.png", resource.getFilename());
        assertArrayEquals(content, resource.getContentAsByteArray());

        store.copy("clinical/lesions/a.png", "clinical/copies/b.png");
        assertArrayEquals(content, Files.readAllBytes(tempDir.resolve("clinical/copies/b.png")));

        store.delete("clinical/lesions/a.png");
        assertFalse(Files.exists(tempDir.resolve("clinical/lesions/a.png")));
        assertDoesNotThrow(() -> store.delete("clinical/lesions/a.png"));
    }

    @Test
    void missingKeyIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> store.get("clinical/missing.png"));
        assertThrows(ResourceNotFoundException.class, () -> store.copy("clinical/missing.png", "clinical/x.png"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../outside.png", "clinical/../../outside.png", "/etc/passwd", "clinical\\a.png",
            "clinical//a.png", "clinical/", "clinical/./a.png", " "})
    void rejectsKeysThatEscapeTheRootOrAreMalformed(String key) {
        assertThrows(IllegalArgumentException.class, () -> store.get(key));
        assertThrows(IllegalArgumentException.class,
                () -> store.put(key, new ByteArrayInputStream(new byte[]{1}), 1, "image/png"));
    }
}
