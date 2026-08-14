package com.clinica.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WebsiteDraftDto {
    @NotBlank @Size(max = 120)
    private String commercialName;
    @Size(max = 180) private String tagline;
    @Size(max = 500) private String description;
    @Size(max = 500) private String logoExternalImageUrl;
    private String logoAssetKey;
    @Size(max = 120) private String heroEyebrow;
    @NotBlank @Size(max = 180) private String heroTitle;
    @Size(max = 120) private String heroHighlight;
    @Size(max = 500) private String heroDescription;
    @Size(max = 80) private String heroPrimaryButtonText;
    @Size(max = 80) private String heroSecondaryButtonText;
    @Size(max = 500) private String heroExternalImageUrl;
    private String heroAssetKey;
    @Size(max = 180) private String approachTitle;
    @Size(max = 120) private String approachHighlight;
    @Size(max = 1000) private String approachDescription;
    @Size(max = 1000) private String approachSecondaryDescription;
    @Size(max = 80) private String approachCtaText;
    @Size(max = 500) private String approachExternalImageUrl;
    private String approachAssetKey;
    @Size(max = 180) private String contactHeading;
    @Size(max = 500) private String contactDescription;
    @Size(max = 40) private String contactPhone;
    @Size(max = 40) private String contactWhatsapp;
    @Email @Size(max = 180) private String contactEmail;
    @Size(max = 300) private String contactAddress;
    @Size(max = 300) private String contactHours;
    @Size(max = 500) private String mapUrl;
    @Size(max = 500) private String facebookUrl;
    @Size(max = 500) private String instagramUrl;
    @Size(max = 500) private String tiktokUrl;
    @Size(max = 500) private String linkedinUrl;
    @Size(max = 180) private String seoTitle;
    @Size(max = 300) private String seoDescription;
    @Size(max = 120) private String seoSiteName;
    @Size(max = 500) private String seoExternalImageUrl;
    private String seoAssetKey;

    @Valid @Size(max = 20)
    private List<Specialty> specialties = new ArrayList<>();
    @Valid @Size(max = 30)
    private List<Benefit> benefits = new ArrayList<>();
    @Valid @Size(max = 30)
    private List<ProcessStep> processSteps = new ArrayList<>();
    @Valid @Size(max = 50)
    private List<Professional> professionals = new ArrayList<>();

    @Data
    public static class Specialty {
        private String draftKey;
        private Long id;
        @NotBlank @Size(max = 50) private String code;
        @NotBlank @Size(max = 120) private String label;
        @NotBlank @Size(max = 180) private String title;
        @Size(max = 500) private String subtitle;
        @NotBlank @Size(max = 40) private String iconCode;
        private int displayOrder;
        private boolean visible = true;
    }

    @Data
    public static class Benefit {
        private String draftKey;
        private Long id;
        @NotBlank @Size(max = 180) private String title;
        @Size(max = 500) private String description;
        @NotBlank @Size(max = 40) private String iconCode;
        private int displayOrder;
        private boolean active = true;
    }

    @Data
    public static class ProcessStep {
        private String draftKey;
        private Long id;
        private int stepNumber;
        @NotBlank @Size(max = 180) private String title;
        @Size(max = 500) private String description;
        private int displayOrder;
        private boolean active = true;
    }

    @Data
    public static class Professional {
        private String draftKey;
        private Long id;
        @NotBlank @Size(max = 180) private String name;
        @Size(max = 120) private String specialty;
        @Size(max = 80) private String licenseNumber;
        @Size(max = 1000) private String description;
        @Size(max = 300) private String experience;
        @Size(max = 500) private String careAreas;
        @Size(max = 500) private String photoExternalUrl;
        private String photoAssetKey;
        private int displayOrder;
        private boolean active = true;
    }
}
