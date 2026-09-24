package com.clinica.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Tipos de archivo aceptados en las subidas, con la firma de bytes que los identifica. La
 * firma impide aceptar, por ejemplo, un SVG o HTML renombrado como {@code .png} con un
 * {@code Content-Type} falso.
 */
public enum UploadFileType {
    PNG("image/png", "PNG", List.of(".png"), new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
    JPEG("image/jpeg", "JPG", List.of(".jpg", ".jpeg"), new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    WEBP("image/webp", "WEBP", List.of(".webp"), "RIFF".getBytes(StandardCharsets.US_ASCII)),
    PDF("application/pdf", "PDF", List.of(".pdf"), "%PDF-".getBytes(StandardCharsets.US_ASCII));

    static final int HEADER_LENGTH = 12;
    private static final byte[] WEBP_MARKER = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private final String contentType;
    private final String displayName;
    private final List<String> extensions;
    private final byte[] signature;

    UploadFileType(String contentType, String displayName, List<String> extensions, byte[] signature) {
        this.contentType = contentType;
        this.displayName = displayName;
        this.extensions = extensions;
        this.signature = signature;
    }

    public String contentType() {
        return contentType;
    }

    public String displayName() {
        return displayName;
    }

    public boolean acceptsExtension(String extension) {
        return extensions.contains(extension);
    }

    public boolean matches(byte[] header) {
        if (header.length < signature.length
                || !Arrays.equals(Arrays.copyOf(header, signature.length), signature)) {
            return false;
        }
        // RIFF es un contenedor genérico (también WAV o AVI): WEBP se identifica en los bytes 8 a 11.
        return this != WEBP || (header.length >= 12
                && Arrays.equals(Arrays.copyOfRange(header, 8, 12), WEBP_MARKER));
    }

    public static Optional<UploadFileType> fromContentType(String contentType) {
        if (contentType == null) return Optional.empty();
        String normalized = contentType.toLowerCase().split(";")[0].trim();
        return Arrays.stream(values()).filter(type -> type.contentType.equals(normalized)).findFirst();
    }
}
