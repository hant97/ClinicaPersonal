package com.clinica.backend.service;

import com.clinica.backend.dto.PublicAppointmentConfirmationDto;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentConfirmationService {

    private static final String CLINIC_DEFAULT_NAME = "Clínica";

    private final AppointmentRepository appointmentRepository;
    private final ClinicSettingsRepository clinicSettingsRepository;

    @Transactional
    public PublicAppointmentConfirmationDto confirmByToken(String token) {
        Appointment appointment = appointmentRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de confirmación no encontrado"));

        String clinicName = clinicSettingsRepository.findTopBySpecialtyAndDeletedFalseOrderByIdAsc(appointment.getSpecialty())
                .map(settings -> settings.getClinicName() != null ? settings.getClinicName() : CLINIC_DEFAULT_NAME)
                .orElse(CLINIC_DEFAULT_NAME);

        PublicAppointmentConfirmationDto.PublicAppointmentConfirmationDtoBuilder builder =
                PublicAppointmentConfirmationDto.builder()
                        .patientName(appointment.getPatient() != null
                                ? (appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName()).trim()
                                : null)
                        .appointmentDate(appointment.getAppointmentDate())
                        .startTime(appointment.getStartTime())
                        .modality(appointment.getModality())
                        .clinicName(clinicName);

        String status = appointment.getStatus();
        if ("CONFIRMADA".equals(status)) {
            return builder.confirmed(true)
                    .alreadyConfirmed(true)
                    .message("La cita ya estaba confirmada.")
                    .build();
        }

        if ("CANCELADA".equals(status) || "COMPLETADA".equals(status) || "NO_ASISTIO".equals(status)) {
            return builder.confirmed(false)
                    .alreadyConfirmed(false)
                    .message("Esta cita se encuentra en estado " + status + " y no puede confirmarse.")
                    .build();
        }

        appointment.setStatus("CONFIRMADA");
        appointment.setConfirmedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        return builder.confirmed(true)
                .alreadyConfirmed(false)
                .message("Cita confirmada exitosamente.")
                .build();
    }
}
