package com.clinica.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "website_professionals")
@Getter
@Setter
@NoArgsConstructor
public class WebsiteProfessional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String specialty;
    private String licenseNumber;
    private String description;
    private String experience;
    private String careAreas;
    private String photoExternalUrl;
    private String photoAssetKey;
    private int displayOrder;
    @Column(name = "is_active", nullable = false)
    private boolean active;
}
