package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "clinic_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "clinic_name")
    private String clinicName;
    
    @Column(name = "short_name")
    private String shortName;
    
    @Column(name = "logo_url")
    private String logoUrl;
    
    @Column(name = "contact_email")
    private String contactEmail;
    
    @Column(name = "contact_phone")
    private String contactPhone;
    
    @Column
    private String address;
    
    @Column(nullable = false)
    private boolean deleted = false;
}
