package com.clinica.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Almacenamiento clínico de imágenes y documentos sobre disco temporal. */
class ClinicalStorageTest {

    @TempDir
    Path tempDir;

    private ClinicalFileStorage images;
    private ClinicalDocumentStorage documents;

    @BeforeEach
    void setUp() {
        LocalBlobStore blobStore = new LocalBlobStore(tempDir.toString());
        images = new ClinicalFileStorage(blobStore);
        documents = new ClinicalDocumentStorage(blobStore);
    }

    @Test
    void storesImagesUnderClinicalNamespaceAndLoadsThemBack() throws Exception {
        String key = images.store(TestUploads.png("lesion.PNG"), "lesions");

        assertTrue(key.matches("lesions/[0-9a-f-]{36}\\.png"));
        assertTrue(Files.exists(tempDir.resolve("clinical").resolve(key)));
        assertArrayEquals(TestUploads.PNG, images.load(key).getContentAsByteArray());
    }

    @Test
    void acceptsEachAllowedImageFormat() {
        images.store(new MockMultipartFile("file", "a.jpg", "image/jpeg", TestUploads.JPEG), "lesions");
        images.store(new MockMultipartFile("file", "a.jpeg", "image/jpeg", TestUploads.JPEG), "lesions");
        images.store(new MockMultipartFile("file", "a.webp", "image/webp", TestUploads.WEBP), "lesions");
    }

    @Test
    void imagesRejectPdfAndDocumentsAcceptIt() {
        assertThrows(IllegalArgumentException.class, () -> images.store(TestUploads.pdf("informe.pdf"), "lesions"));

        String key = documents.store(TestUploads.pdf("informe.pdf"), "documents");
        assertTrue(key.startsWith("documents/") && key.endsWith(".pdf"));
    }

    @Test
    void rejectsExtensionThatDoesNotMatchType() {
        MockMultipartFile jpegNamedPng = new MockMultipartFile("file", "a.png", "image/jpeg", TestUploads.JPEG);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> images.store(jpegNamedPng, "lesions"));
        assertEquals("La extensión del archivo no coincide con su tipo", error.getMessage());
    }

    @Test
    void rejectsRiffContainersThatAreNotWebp() {
        byte[] wav = "RIFF\0\0\0\0WAVEfmt ".getBytes();
        MockMultipartFile fakeWebp = new MockMultipartFile("file", "a.webp", "image/webp", wav);

        assertThrows(IllegalArgumentException.class, () -> images.store(fakeWebp, "lesions"));
    }

    @Test
    void rejectsPdfLargerThanFiveMegabytes() {
        byte[] content = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(TestUploads.PDF, 0, content, 0, TestUploads.PDF.length);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> documents.store(new MockMultipartFile("file", "a.pdf", "application/pdf", content), "documents"));
        assertEquals("El archivo no debe exceder 5MB", error.getMessage());
    }

    @Test
    void rejectsFilesWithoutExtensionOrEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> images.store(new MockMultipartFile("file", "lesion", "image/png", TestUploads.PNG), "lesions"));
        assertThrows(IllegalArgumentException.class,
                () -> images.store(new MockMultipartFile("file", "a.png", "image/png", new byte[0]), "lesions"));
    }

    @Test
    void rejectsTraversalInCategoryAndKeys() {
        assertThrows(IllegalArgumentException.class, () -> images.store(TestUploads.png("a.png"), "../website"));
        assertThrows(IllegalArgumentException.class, () -> images.load("../website/hero/a.png"));
        assertThrows(IllegalArgumentException.class, () -> documents.delete("../../etc/passwd"));
    }
}
