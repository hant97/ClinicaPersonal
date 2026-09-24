package com.clinica.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Imágenes clínicas privadas: fotos de lesiones y fotos de pacientes. */
@Service
public class ClinicalFileStorage extends ValidatedBlobStorage {

    public ClinicalFileStorage(BlobStore blobStore) {
        super(blobStore, "clinical", UploadRules.IMAGES_2MB);
    }

    public String store(MultipartFile file, String category) {
        return storeValidated(file, category);
    }
}
