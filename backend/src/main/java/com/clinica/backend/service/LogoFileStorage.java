package com.clinica.backend.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Logos de la clínica. Las claves son nombres de archivo sin subcarpetas. */
@Service
public class LogoFileStorage extends ValidatedBlobStorage {

    public LogoFileStorage(BlobStore blobStore) {
        super(blobStore, "logos", UploadRules.IMAGES_2MB);
    }

    public String store(MultipartFile file) {
        return storeValidated(file, null);
    }

    @Override
    public Resource load(String filename) {
        if (filename == null || filename.contains("/")) {
            throw new IllegalArgumentException("Ruta de archivo inválida");
        }
        return super.load(filename);
    }
}
