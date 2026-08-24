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

    @Transactional(readOnly = true)
    public PublicAppointmentConfirmationDto getConfirmationByToken(String token) {
        Appointment appointment = appointmentRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de confirmación no encontrado"));

        return responseForCurrentStatus(appointment);
    }

    @Transactional
    public PublicAppointmentConfirmationDto confirmByToken(String token) {
        Appointment appointment = appointmentRepository.findByConfirmationTokenForUpdate(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de confirmación no encontrado"));

        if ("CONFIRMADA".equals(appointment.getStatus())) {
            return responseBuilder(appointment)
                    .confirmed(true)
                    .alreadyConfirmed(true)
                    .confirmable(false)
                    .message("La cita ya estaba confirmada.")
                    .build();
        }

        if (!"PROGRAMADA".equals(appointment.getStatus())) {
            return nonConfirmableResponse(appointment);
        }

        appointment.setStatus("CONFIRMADA");
        appointment.setConfirmedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        return responseBuilder(appointment)
                .confirmed(true)
                .alreadyConfirmed(false)
                .confirmable(false)
                .message("Cita confirmada exitosamente.")
                .build();
    }

    private PublicAppointmentConfirmationDto responseForCurrentStatus(Appointment appointment) {
        if ("CONFIRMADA".equals(appointment.getStatus())) {
            return responseBuilder(appointment)
                    .confirmed(true)
                    .alreadyConfirmed(true)
                    .confirmable(false)
                    .message("La cita ya estaba confirmada.")
                    .build();
        }

        if ("PROGRAMADA".equals(appointment.getStatus())) {
            return responseBuilder(appointment)
                    .confirmed(false)
                    .alreadyConfirmed(false)
                    .confirmable(true)
                    .message("La cita está pendiente de confirmación.")
                    .build();
        }

        return nonConfirmableResponse(appointment);
    }

    private PublicAppointmentConfirmationDto nonConfirmableResponse(Appointment appointment) {
        return responseBuilder(appointment)
                .confirmed(false)
                .alreadyConfirmed(false)
                .confirmable(false)
                .message("Esta cita se encuentra en estado " + appointment.getStatus() + " y no puede confirmarse.")
                .build();
    }

    private PublicAppointmentConfirmationDto.PublicAppointmentConfirmationDtoBuilder responseBuilder(
            Appointment appointment) {
        String clinicName = clinicSettingsRepository
                .findTopBySpecialtyAndDeletedFalseOrderByIdAsc(appointment.getSpecialty())
                .map(settings -> settings.getClinicName() != null
                        ? settings.getClinicName()
                        : CLINIC_DEFAULT_NAME)
                .orElse(CLINIC_DEFAULT_NAME);

        return PublicAppointmentConfirmationDto.builder()
                .patientName(appointment.getPatient() != null
                        ? (appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName()).trim()
                        : null)
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .modality(appointment.getModality())
                .clinicName(clinicName);
    }
}
