package com.clinica.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PatientDto {
    private Long id;
    private UUID uuid;
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 50, message = "El nombre no debe superar 50 caracteres")
    private String firstName;
    @NotBlank(message = "El apellido es obligatorio") @Size(max = 50, message = "El apellido no debe superar 50 caracteres")
    private String lastName;
    @NotBlank(message = "El documento de identidad es obligatorio")
    @Size(max = 20, message = "El documento de identidad no debe superar 20 caracteres")
    private String identificationDocument;
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate dateOfBirth;
    @Size(max = 20, message = "El teléfono no debe superar 20 caracteres")
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
    @Size(max = 500, message = "La URL de la foto es demasiado larga")
    private String photoUrl;
    private Boolean active;
    private String specialty;
    private boolean hasActiveAlerts;
    private boolean deleted;
    private LocalDateTime createdAt;
}
