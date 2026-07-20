package com.clinica.backend.dto;

import lombok.Data;

@Data
public class ClinicSettingsDto {
    private Long id;
    private String clinicName;
    private String shortName;
    private String logoUrl;
    private String contactEmail;
    private String contactPhone;
    private String address;
}
