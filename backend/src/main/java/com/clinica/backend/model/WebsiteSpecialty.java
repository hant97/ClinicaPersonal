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
@Table(name = "website_specialties")
@Getter
@Setter
@NoArgsConstructor
public class WebsiteSpecialty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String label;
    private String title;
    private String subtitle;
    private String iconCode;
    private int displayOrder;
    @Column(name = "is_visible", nullable = false)
    private boolean visible;
}
