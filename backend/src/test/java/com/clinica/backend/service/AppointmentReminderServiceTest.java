package com.clinica.backend.service;

import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AppointmentReminderServiceTest {

    private AppointmentRepository appointmentRepository;
    private EmailService emailService;
    private ClinicSettingsRepository clinicSettingsRepository;
    private AppointmentReminderService reminderService;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        emailService = mock(EmailService.class);
        clinicSettingsRepository = mock(ClinicSettingsRepository.class);
        reminderService = new AppointmentReminderService(appointmentRepository, emailService, clinicSettingsRepository);
        ReflectionTestUtils.setField(reminderService, "reminderLeadDays", 1);
        ReflectionTestUtils.setField(reminderService, "publicBaseUrl", "http://localhost:4200");
    }

    private Appointment buildAppointment(String email, String token) {
        Patient patient = new Patient();
        patient.setFirstName("Ana");
        patient.setLastName("Gomez");
        patient.setEmail(email);

        Appointment appointment = new Appointment();
        appointment.setId(10L);
        appointment.setPatient(patient);
        appointment.setSpecialty("PSICOLOGIA");
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        appointment.setStatus("PROGRAMADA");
        appointment.setConfirmationToken(token);
        return appointment;
    }

    @Test
    void shouldSkipWhenMailNotConfigured() {
        when(emailService.isConfigured()).thenReturn(false);

        reminderService.sendDueReminders();

        verify(appointmentRepository, never()).findPendingRemindersForDate(any());
        verify(emailService, never()).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void shouldSendReminderAndMarkReminderSent() {
        Appointment appointment = buildAppointment("ana@example.com", "token-123");
        when(emailService.isConfigured()).thenReturn(true);
        when(appointmentRepository.findPendingRemindersForDate(any(LocalDate.class)))
                .thenReturn(List.of(appointment));
        when(emailService.sendHtml(anyString(), anyString(), anyString())).thenReturn(true);

        reminderService.sendDueReminders();

        verify(emailService).sendHtml(eq("ana@example.com"), contains("Recordatorio"), contains("confirmar-cita/token-123"));
        assertNotNull(appointment.getReminderSentAt());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void shouldSkipPatientWithoutEmail() {
        Appointment appointment = buildAppointment(null, "token-123");
        when(emailService.isConfigured()).thenReturn(true);
        when(appointmentRepository.findPendingRemindersForDate(any(LocalDate.class)))
                .thenReturn(List.of(appointment));

        reminderService.sendDueReminders();

        verify(emailService, never()).sendHtml(anyString(), anyString(), anyString());
        assertNull(appointment.getReminderSentAt());
    }

    @Test
    void shouldSkipAppointmentWithoutToken() {
        Appointment appointment = buildAppointment("ana@example.com", null);
        when(emailService.isConfigured()).thenReturn(true);
        when(appointmentRepository.findPendingRemindersForDate(any(LocalDate.class)))
                .thenReturn(List.of(appointment));

        reminderService.sendDueReminders();

        verify(emailService, never()).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void shouldNotMarkWhenSendingFails() {
        Appointment appointment = buildAppointment("ana@example.com", "token-123");
        when(emailService.isConfigured()).thenReturn(true);
        when(appointmentRepository.findPendingRemindersForDate(any(LocalDate.class)))
                .thenReturn(List.of(appointment));
        when(emailService.sendHtml(anyString(), anyString(), anyString())).thenReturn(false);

        reminderService.sendDueReminders();

        assertNull(appointment.getReminderSentAt());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }
}
