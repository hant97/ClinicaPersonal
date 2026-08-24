package com.clinica.backend.service;

import com.clinica.backend.config.GoogleCalendarProperties;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class GoogleCalendarServiceTest {

    private GoogleCalendarProperties properties;
    private GoogleCalendarService service;

    @BeforeEach
    void setUp() {
        properties = new GoogleCalendarProperties();
        properties.setEnabled(false);
        service = new GoogleCalendarService(properties);
    }

    @Test
    void syncAppointmentShouldDoNothingWhenDisabled() {
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setAppointmentDate(LocalDate.of(2026, 9, 1));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));

        service.syncAppointment(appointment);

        assertNull(appointment.getGoogleEventId());
        assertNull(appointment.getGoogleEventLink());
    }

    @Test
    void cancelAppointmentEventShouldDoNothingWhenDisabled() {
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setGoogleEventId("existing-event-id");

        service.cancelAppointmentEvent(appointment);

        // Does not throw and leaves state unchanged when disabled
        assertEquals("existing-event-id", appointment.getGoogleEventId());
    }

    @Test
    void syncAppointmentShouldHandleMissingCredentialsGracefullyWhenEnabled() {
        properties.setEnabled(true);
        properties.setCredentialsPath("non-existent-path-12345.json");
        properties.setCredentialsJson(null);

        Appointment appointment = new Appointment();
        appointment.setId(2L);
        appointment.setAppointmentDate(LocalDate.of(2026, 9, 1));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        Patient patient = new Patient();
        patient.setFirstName("Carlos");
        patient.setLastName("Mendoza");
        appointment.setPatient(patient);

        assertDoesNotThrow(() -> service.syncAppointment(appointment));
        assertNull(appointment.getGoogleEventId());
    }

    @Test
    void testResolvingCredentialsPathWhenFileExists(@TempDir Path tempDir) throws Exception {
        String credentials = "{\"type\":\"service_account\"}";
        Path credentialsFile = tempDir.resolve("google-credentials.json");
        Files.writeString(credentialsFile, credentials, StandardCharsets.UTF_8);

        properties.setEnabled(true);
        properties.setCredentialsPath(credentialsFile.toString());

        try (InputStream resolved = service.resolveCredentialsStream()) {
            assertNotNull(resolved);
            assertEquals(credentials, new String(resolved.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}

