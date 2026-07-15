package com.clinica.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String identificationDocument;
    private LocalDate dateOfBirth;
    private String contactNumber;
    private String email;
    private String occupation;
    private String maritalStatus;
    private String emergencyContact;
    private String reasonForConsultation;
    private String gender;
    private String address;
    private String guardianName;
    private String guardianContact;
    private boolean deleted;
}
