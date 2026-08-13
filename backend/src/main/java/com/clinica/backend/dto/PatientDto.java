package com.clinica.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PatientDto {
    private Long id;
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
    private String firstName;
    @NotBlank(message = "El apellido es obligatorio") @Size(max = 100, message = "El apellido no debe superar 100 caracteres")
    private String lastName;
    private String identificationDocument;
    @NotNull(message = "La fecha de nacimiento es obligatoria") @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate dateOfBirth;
    private String contactNumber;
    @Email(message = "El correo electrónico no tiene un formato válido") @Size(max = 150, message = "El correo no debe superar 150 caracteres")
    private String email;
    private String occupation;
    private String maritalStatus;
    private String emergencyContact;
    @Size(max = 500, message = "El motivo de consulta no debe superar 500 caracteres")
    private String reasonForConsultation;
    @NotBlank(message = "El género es obligatorio")
    private String gender;
    private String address;
    private String guardianName;
    private String guardianContact;
    private boolean hasLegalGuardian;
    private String specialty;
    private boolean hasActiveAlerts;
    private boolean deleted;
    private LocalDateTime createdAt;
}
