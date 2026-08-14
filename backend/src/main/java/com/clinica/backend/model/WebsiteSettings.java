package com.clinica.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "website_settings")
@Getter
@Setter
@NoArgsConstructor
public class WebsiteSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "singleton_key", nullable = false, unique = true, length = 1)
    private String singletonKey = "S";
    private String commercialName;
    private String tagline;
    private String description;
    private String logoExternalImageUrl;
    private String logoAssetKey;
    private String heroEyebrow;
    private String heroTitle;
    private String heroHighlight;
    private String heroDescription;
    private String heroPrimaryButtonText;
    private String heroSecondaryButtonText;
    private String heroExternalImageUrl;
    private String heroAssetKey;
    private String approachTitle;
    private String approachHighlight;
    private String approachDescription;
    private String approachSecondaryDescription;
    private String approachCtaText;
    private String approachExternalImageUrl;
    private String approachAssetKey;
    private String contactHeading;
    private String contactDescription;
    private String contactPhone;
    private String contactWhatsapp;
    private String contactEmail;
    private String contactAddress;
    private String contactHours;
    private String mapUrl;
    private String facebookUrl;
    private String instagramUrl;
    private String tiktokUrl;
    private String linkedinUrl;
    private String seoTitle;
    private String seoDescription;
    private String seoSiteName;
    private String seoExternalImageUrl;
    private String seoAssetKey;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "published_revision", nullable = false)
    private long publishedRevision;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}
