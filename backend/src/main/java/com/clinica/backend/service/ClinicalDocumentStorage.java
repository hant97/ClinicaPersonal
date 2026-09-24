package com.clinica.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Documentos clínicos privados (PDF e imágenes): consentimientos, informes, resultados. */
@Service
public class ClinicalDocumentStorage extends ValidatedBlobStorage {

    public ClinicalDocumentStorage(BlobStore blobStore) {
        super(blobStore, "clinical", UploadRules.CLINICAL_DOCUMENTS_5MB);
    }

    public String store(MultipartFile file, String category) {
        return storeValidated(file, category);
    }
}
