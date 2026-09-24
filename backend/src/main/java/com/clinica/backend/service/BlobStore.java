package com.clinica.backend.service;

import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.exception.StorageException;
import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Almacén de archivos por clave, independiente del medio físico. La implementación se elige
 * con {@code storage.provider}: {@code local} (disco, por defecto) o {@code s3} (AWS S3,
 * Cloudflare R2 u otro servicio compatible).
 * <p>
 * Las claves son rutas relativas con {@code /} como separador (por ejemplo
 * {@code clinical/lesions/uuid.jpg}) y se validan con {@link #requireValidKey(String)}.
 * Los fallos de E/S se lanzan como {@link StorageException}.
 */
public interface BlobStore {

    void put(String key, InputStream content, long size, String contentType);

    /** @throws ResourceNotFoundException si la clave no existe. */
    Resource get(String key);

    /** Idempotente: no falla si la clave no existe. */
    void delete(String key);

    void copy(String sourceKey, String targetKey);

    static String requireValidKey(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.endsWith("/")
                || key.contains("\\") || key.contains("//")) {
            throw new IllegalArgumentException("Ruta de archivo inválida");
        }
        for (String segment : key.split("/")) {
            if (segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Ruta de archivo inválida");
            }
        }
        for (int i = 0; i < key.length(); i++) {
            if (Character.isISOControl(key.charAt(i))) {
                throw new IllegalArgumentException("Ruta de archivo inválida");
            }
        }
        return key;
    }

    static String filenameOf(String key) {
        return key.substring(key.lastIndexOf('/') + 1);
    }
}
