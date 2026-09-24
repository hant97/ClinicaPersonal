package com.clinica.backend.service;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Tipos y tamaño máximo admitidos por un almacenamiento concreto. */
public record UploadRules(Set<UploadFileType> types, long maxBytes) {

    public static final UploadRules IMAGES_2MB = new UploadRules(
            EnumSet.of(UploadFileType.PNG, UploadFileType.JPEG, UploadFileType.WEBP), 2L * 1024 * 1024);

    public static final UploadRules CLINICAL_DOCUMENTS_5MB = new UploadRules(
            EnumSet.of(UploadFileType.PDF, UploadFileType.PNG, UploadFileType.JPEG, UploadFileType.WEBP), 5L * 1024 * 1024);

    public UploadRules {
        types = Set.copyOf(types);
    }

    String maxSizeLabel() {
        return (maxBytes / (1024 * 1024)) + "MB";
    }

    String allowedTypesLabel() {
        return types.stream().sorted().map(UploadFileType::displayName).collect(Collectors.joining(", "));
    }
}
