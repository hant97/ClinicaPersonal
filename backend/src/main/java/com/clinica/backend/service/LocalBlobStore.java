package com.clinica.backend.service;

import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Almacén en disco. Adecuado para desarrollo o para un servidor con disco persistente; en un
 * contenedor efímero (por ejemplo, Render sin disco) los archivos se pierden en cada deploy.
 */
@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalBlobStore implements BlobStore {

    private static final Logger log = LoggerFactory.getLogger(LocalBlobStore.class);

    private final Path root;

    public LocalBlobStore(@Value("${storage.local.root:uploads}") String root) {
        Path configured = Paths.get(root);
        this.root = configured.toAbsolutePath().normalize();
        if (!configured.isAbsolute()) {
            log.warn("storage.local.root es relativo y depende del directorio de arranque; se usará {}. "
                    + "Defina STORAGE_LOCAL_ROOT con una ruta absoluta.", this.root);
        } else {
            log.info("Almacenamiento local de archivos en {}", this.root);
        }
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        Path destination = resolve(key);
        try {
            Files.createDirectories(destination.getParent());
            Files.copy(content, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new StorageException("No se pudo almacenar el archivo", ex);
        }
    }

    @Override
    public Resource get(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException("Archivo no encontrado");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            throw new StorageException("No se pudo eliminar el archivo", ex);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey) {
        Path source = resolve(sourceKey);
        Path target = resolve(targetKey);
        if (!Files.isRegularFile(source)) {
            throw new ResourceNotFoundException("Archivo no encontrado");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new StorageException("No se pudo copiar el archivo", ex);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(BlobStore.requireValidKey(key)).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("Ruta de archivo inválida");
        }
        return path;
    }
}
