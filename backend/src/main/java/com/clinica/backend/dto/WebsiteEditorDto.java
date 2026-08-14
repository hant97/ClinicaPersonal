package com.clinica.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WebsiteEditorDto {
    private WebsiteDraftDto draft;
    private long revision;
    private boolean hasUnpublishedChanges;
    private LocalDateTime draftUpdatedAt;
    private Long draftUpdatedBy;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private long publishedRevision;
}
