package com.clinica.backend.service;

import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Archivos de prueba con la firma de bytes real de cada formato. */
final class TestUploads {

    static final byte[] PNG = withBody(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
    static final byte[] JPEG = withBody(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
    static final byte[] WEBP = withBody("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII));
    static final byte[] PDF = withBody("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII));

    private TestUploads() {
    }

    static MockMultipartFile png(String filename) {
        return new MockMultipartFile("file", filename, "image/png", PNG);
    }

    static MockMultipartFile pdf(String filename) {
        return new MockMultipartFile("file", filename, "application/pdf", PDF);
    }

    private static byte[] withBody(byte[] header) {
        byte[] content = Arrays.copyOf(header, header.length + 32);
        Arrays.fill(content, header.length, content.length, (byte) 7);
        return content;
    }
}
