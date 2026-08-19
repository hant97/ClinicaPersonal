package com.clinica.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppointmentDto {
    private Long id;
    @NotNull(message = "El paciente es obligatorio") private Long patientId;
    private String patientUuid;
    private String patientName;
    @NotNull(message = "La fecha de cita es obligatoria")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;
    @NotNull(message = "La hora de inicio es obligatoria")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm[:ss]")
    private LocalTime startTime;
    @NotNull(message = "La hora de fin es obligatoria")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm[:ss]")
    private LocalTime endTime;

    @NotBlank(message = "El estado es obligatorio") @Size(max = 50, message = "El estado es demasiado largo") private String status;
    @NotBlank(message = "La modalidad es obligatoria") @Size(max = 50, message = "La modalidad es demasiado larga") private String modality;
    private String videoCallLink;
    private Long professionalId;
    @JsonProperty("isFirstTime")
    private Boolean isFirstTime = false;
    private Long clinicalSessionId;
    private Long clinicalServiceId;
    private String clinicalServiceName;
    private String notes;
    private String specialty;
    @JsonProperty("isPaid")
    private Boolean isPaid = false;
    private Long paymentId;
    private java.math.BigDecimal paymentAmount;
    private String googleEventId;
    private String googleEventLink;

    public boolean isFirstTime() {
        return Boolean.TRUE.equals(this.isFirstTime);
    }

    public void setFirstTime(Boolean firstTime) {
        this.isFirstTime = Boolean.TRUE.equals(firstTime);
    }

    public boolean isPaid() {
        return Boolean.TRUE.equals(this.isPaid);
    }

    public void setPaid(Boolean paid) {
        this.isPaid = Boolean.TRUE.equals(paid);
    }
}



