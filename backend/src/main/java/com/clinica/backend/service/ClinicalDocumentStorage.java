package com.clinica.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class ClinicalDocumentStorage {
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".png", ".jpg", ".jpeg", ".webp");
    private static final List<String> ALLOWED_TYPES = List.of("application/pdf", "image/png", "image/jpeg", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final Path root;

    public ClinicalDocumentStorage(@Value("${clinical.storage.root:uploads/clinical}") String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, String category) {
        validate(file);
        String extension = extension(file.getOriginalFilename());
        Path categoryPath = safePath(category);
        try {
            Files.createDirectories(categoryPath);
            String key = category + "/" + UUID.randomUUID() + extension;
            Path destination = safePath(key);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo almacenar el documento", ex);
        }
    }

    public Resource load(String key) {
        try {
            Path path = safePath(key);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Documento no encontrado");
            }
            return resource;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Documento no encontrado", ex);
        }
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            Files.deleteIfExists(safePath(key));
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo eliminar el documento", ex);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("El documento no puede estar vacío");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("El documento no debe exceder 5MB");
        String type = file.getContentType();
        if (type == null || !ALLOWED_TYPES.contains(type.toLowerCase())) throw new IllegalArgumentException("Tipo de documento no permitido");
        if (!ALLOWED_EXTENSIONS.contains(extension(file.getOriginalFilename()))) throw new IllegalArgumentException("Extensión de documento no permitida");
    }

    private String extension(String filename) {
        if (filename == null) throw new IllegalArgumentException("El nombre del documento es inválido");
        int dot = filename.lastIndexOf('.');
        if (dot < 0) throw new IllegalArgumentException("El documento debe tener extensión");
        return filename.substring(dot).toLowerCase();
    }

    private Path safePath(String key) {
        if (key == null || key.isBlank() || key.contains("..") || key.contains("\\") || key.startsWith("/")) {
            throw new IllegalArgumentException("Ruta de documento inválida");
        }
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("Ruta de documento inválida");
        return path;
    }
}
