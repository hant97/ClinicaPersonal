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
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class LocalWebsiteFileStorage implements WebsiteFileStorage {
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".webp");
    private static final List<String> ALLOWED_TYPES = List.of("image/png", "image/jpeg", "image/webp");
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    private static final String DRAFT_PREFIX = "draft/";

    private final Path root;

    public LocalWebsiteFileStorage(@Value("${website.storage.root:uploads/website}") String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String category, String previousKey) {
        validate(file);
        String extension = extension(file.getOriginalFilename());
        Path categoryPath = safePath(category);
        try {
            Files.createDirectories(categoryPath);
            String key = category + "/" + UUID.randomUUID() + extension;
            Path destination = safePath(key);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            delete(previousKey);
            return key;
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo almacenar la imagen", ex);
        }
    }

    @Override
    public String storeDraft(MultipartFile file, String category) {
        validate(file);
        String extension = extension(file.getOriginalFilename());
        try {
            Path categoryPath = safePath(DRAFT_PREFIX + category);
            Files.createDirectories(categoryPath);
            String key = DRAFT_PREFIX + category + "/" + UUID.randomUUID() + extension;
            Path destination = safePath(key);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo almacenar la imagen", ex);
        }
    }

    @Override
    public String promote(String draftKey) {
        if (draftKey == null || draftKey.isBlank() || !isDraftKey(draftKey)) return draftKey;
        String publishedKey = draftKey.substring(DRAFT_PREFIX.length());
        try {
            Path source = safePath(draftKey);
            Path target = safePath(publishedKey);
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return publishedKey;
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo publicar la imagen", ex);
        }
    }

    @Override
    public boolean isDraftKey(String key) {
        return key != null && key.startsWith(DRAFT_PREFIX);
    }

    @Override
    public Resource load(String key) {
        try {
            Path path = safePath(key);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Imagen no encontrada");
            }
            return resource;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Imagen no encontrada", ex);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            Files.deleteIfExists(safePath(key));
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo eliminar la imagen", ex);
        }
    }

    @Override
    public String publicUrl(String key) {
        if (key == null || key.isBlank()) return null;
        safePath(key);
        return "/api/v1/public/website-assets/" + key;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("La imagen no puede estar vacía");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("La imagen no debe exceder 2MB");
        String type = file.getContentType();
        if (type == null || !ALLOWED_TYPES.contains(type.toLowerCase())) throw new IllegalArgumentException("Tipo de imagen no permitido");
        if (!ALLOWED_EXTENSIONS.contains(extension(file.getOriginalFilename()))) throw new IllegalArgumentException("Extensión de imagen no permitida");
    }

    private String extension(String filename) {
        if (filename == null) throw new IllegalArgumentException("El nombre de imagen es inválido");
        int dot = filename.lastIndexOf('.');
        if (dot < 0) throw new IllegalArgumentException("La imagen debe tener extensión");
        return filename.substring(dot).toLowerCase();
    }

    private Path safePath(String key) {
        if (key == null || key.isBlank() || key.contains("..") || key.contains("\\") || key.startsWith("/")) {
            throw new IllegalArgumentException("Ruta de imagen inválida");
        }
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("Ruta de imagen inválida");
        return path;
    }
}
