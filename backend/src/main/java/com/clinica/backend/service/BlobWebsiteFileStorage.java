package com.clinica.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Imágenes del sitio web público e inventario. Se sirven sin autenticación mediante
 * {@code /api/v1/public/website-assets/}, así que no deben contener datos de pacientes
 * (para eso existe {@link ClinicalFileStorage}).
 */
@Service
public class BlobWebsiteFileStorage extends ValidatedBlobStorage implements WebsiteFileStorage {

    private static final String DRAFT_PREFIX = "draft/";
    private static final String PUBLIC_URL_PREFIX = "/api/v1/public/website-assets/";

    public BlobWebsiteFileStorage(BlobStore blobStore) {
        super(blobStore, "website", UploadRules.IMAGES_2MB);
    }

    @Override
    public String store(MultipartFile file, String category, String previousKey) {
        String key = storeValidated(file, category);
        delete(previousKey);
        return key;
    }

    @Override
    public String storeDraft(MultipartFile file, String category) {
        return storeValidated(file, DRAFT_PREFIX + category);
    }

    @Override
    public String promote(String draftKey) {
        if (draftKey == null || draftKey.isBlank() || !isDraftKey(draftKey)) return draftKey;
        String publishedKey = draftKey.substring(DRAFT_PREFIX.length());
        blobStore.copy(namespaced(draftKey), namespaced(publishedKey));
        return publishedKey;
    }

    @Override
    public boolean isDraftKey(String key) {
        return key != null && key.startsWith(DRAFT_PREFIX);
    }

    @Override
    public String publicUrl(String key) {
        if (key == null || key.isBlank()) return null;
        return PUBLIC_URL_PREFIX + BlobStore.requireValidKey(key);
    }
}
