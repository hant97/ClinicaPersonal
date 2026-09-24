package com.clinica.backend.service;

import com.clinica.backend.exception.StorageException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Base de los almacenamientos por dominio (clínico, documentos, sitio web, logos): valida el
 * archivo, genera una clave aleatoria y delega en {@link BlobStore} bajo un espacio de nombres
 * propio. Las claves que se devuelven y se guardan en la base son relativas a ese espacio.
 */
public abstract class ValidatedBlobStorage {

    protected final BlobStore blobStore;
    private final String namespace;
    private final UploadRules rules;

    protected ValidatedBlobStorage(BlobStore blobStore, String namespace, UploadRules rules) {
        this.blobStore = blobStore;
        this.namespace = namespace.endsWith("/") ? namespace : namespace + "/";
        this.rules = rules;
    }

    public Resource load(String key) {
        return blobStore.get(namespaced(key));
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        blobStore.delete(namespaced(key));
    }

    /** @param directory subcarpeta dentro del espacio de nombres, o {@code null} para la raíz. */
    protected String storeValidated(MultipartFile file, String directory) {
        UploadFileType type = validate(file);
        String prefix = directory == null ? "" : BlobStore.requireValidKey(directory) + "/";
        String key = prefix + UUID.randomUUID() + extension(file.getOriginalFilename());
        try (InputStream content = file.getInputStream()) {
            blobStore.put(namespaced(key), content, file.getSize(), type.contentType());
        } catch (IOException ex) {
            throw new StorageException("No se pudo leer el archivo recibido", ex);
        }
        return key;
    }

    protected String namespaced(String key) {
        return namespace + BlobStore.requireValidKey(key);
    }

    private UploadFileType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        if (file.getSize() > rules.maxBytes()) {
            throw new IllegalArgumentException("El archivo no debe exceder " + rules.maxSizeLabel());
        }
        UploadFileType type = UploadFileType.fromContentType(file.getContentType())
                .filter(rules.types()::contains)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de archivo no permitido. Formatos aceptados: " + rules.allowedTypesLabel()));
        if (!type.acceptsExtension(extension(file.getOriginalFilename()))) {
            throw new IllegalArgumentException("La extensión del archivo no coincide con su tipo");
        }
        if (!type.matches(readHeader(file))) {
            throw new IllegalArgumentException("El contenido del archivo no coincide con su tipo");
        }
        return type;
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream content = file.getInputStream()) {
            return content.readNBytes(UploadFileType.HEADER_LENGTH);
        } catch (IOException ex) {
            throw new StorageException("No se pudo leer el archivo recibido", ex);
        }
    }

    private static String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new IllegalArgumentException("El archivo debe tener un nombre con extensión");
        }
        return filename.substring(dot).toLowerCase();
    }
}
