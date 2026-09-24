package com.clinica.backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface WebsiteFileStorage {
    String store(MultipartFile file, String category, String previousKey);
    Resource load(String key);
    void delete(String key);
    String publicUrl(String key);

    String storeDraft(MultipartFile file, String category);
    String promote(String draftKey);
    boolean isDraftKey(String key);
}
