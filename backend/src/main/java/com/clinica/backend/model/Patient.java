package com.clinica.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "identification_document", unique = true)
    private String identificationDocument;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "reason_for_consultation", length = 500)
    private String reasonForConsultation;

    @Column(name = "gender")
    private String gender;

    @Column(name = "address")
    private String address;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "guardian_contact")
    private String guardianContact;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
