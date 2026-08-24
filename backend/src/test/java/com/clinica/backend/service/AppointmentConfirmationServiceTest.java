package com.clinica.backend.service;

import com.clinica.backend.dto.PublicAppointmentConfirmationDto;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.ClinicSettings;
import com.clinica.backend.model.Patient;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppointmentConfirmationServiceTest {

    private AppointmentRepository appointmentRepository;
    private ClinicSettingsRepository clinicSettingsRepository;
    private AppointmentConfirmationService confirmationService;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        clinicSettingsRepository = mock(ClinicSettingsRepository.class);
        confirmationService = new AppointmentConfirmationService(appointmentRepository, clinicSettingsRepository);
    }

    private Appointment buildAppointment(String status) {
        Patient patient = new Patient();
        patient.setFirstName("Ana");
        patient.setLastName("Gomez");

        Appointment appointment = new Appointment();
        appointment.setId(10L);
        appointment.setPatient(patient);
        appointment.setSpecialty("PSICOLOGIA");
        appointment.setAppointmentDate(LocalDate.of(2026, 9, 1));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        appointment.setStatus(status);
        appointment.setConfirmationToken("token-123");
        appointment.setModality("PRESENCIAL");
        return appointment;
    }

    @Test
    void confirmShouldMoveProgramadaToConfirmada() {
        Appointment appointment = buildAppointment("PROGRAMADA");
        ClinicSettings settings = new ClinicSettings();
        settings.setClinicName("Clínica Vida Saludable");
        when(appointmentRepository.findByConfirmationTokenForUpdate("token-123")).thenReturn(Optional.of(appointment));
        when(clinicSettingsRepository.findTopBySpecialtyAndDeletedFalseOrderByIdAsc("PSICOLOGIA"))
                .thenReturn(Optional.of(settings));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicAppointmentConfirmationDto result = confirmationService.confirmByToken("token-123");

        assertTrue(result.isConfirmed());
        assertFalse(result.isAlreadyConfirmed());
        assertFalse(result.isConfirmable());
        assertEquals("Ana Gomez", result.getPatientName());
        assertEquals("Clínica Vida Saludable", result.getClinicName());
        assertEquals("CONFIRMADA", appointment.getStatus());
        assertNotNull(appointment.getConfirmedAt());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void confirmShouldReportAlreadyConfirmedWithoutSaving() {
        Appointment appointment = buildAppointment("CONFIRMADA");
        when(appointmentRepository.findByConfirmationTokenForUpdate("token-123")).thenReturn(Optional.of(appointment));

        PublicAppointmentConfirmationDto result = confirmationService.confirmByToken("token-123");

        assertTrue(result.isConfirmed());
        assertTrue(result.isAlreadyConfirmed());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void confirmShouldRejectCancelledAppointment() {
        Appointment appointment = buildAppointment("CANCELADA");
        when(appointmentRepository.findByConfirmationTokenForUpdate("token-123")).thenReturn(Optional.of(appointment));

        PublicAppointmentConfirmationDto result = confirmationService.confirmByToken("token-123");

        assertFalse(result.isConfirmed());
        assertTrue(result.getMessage().contains("CANCELADA"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void confirmShouldThrowWhenTokenNotFound() {
        when(appointmentRepository.findByConfirmationTokenForUpdate("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> confirmationService.confirmByToken("missing"));
    }

    @Test
    void getShouldReturnPreviewWithoutChangingAppointment() {
        Appointment appointment = buildAppointment("PROGRAMADA");
        when(appointmentRepository.findByConfirmationToken("token-123")).thenReturn(Optional.of(appointment));

        PublicAppointmentConfirmationDto result = confirmationService.getConfirmationByToken("token-123");

        assertFalse(result.isConfirmed());
        assertFalse(result.isAlreadyConfirmed());
        assertTrue(result.isConfirmable());
        assertEquals("PROGRAMADA", appointment.getStatus());
        assertNull(appointment.getConfirmedAt());
        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(appointmentRepository, never()).findByConfirmationTokenForUpdate(any());
    }
}
