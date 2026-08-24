package com.clinica.backend.service;

import com.clinica.backend.config.GoogleCalendarProperties;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    private final GoogleCalendarProperties properties;

    /**
     * Sincroniza una cita médica con Google Calendar.
     * Crea un nuevo evento o actualiza el existente si ya cuenta con googleEventId.
     * En caso de error o si la integración está deshabilitada, no interrumpe el flujo de la cita.
     */
    public void syncAppointment(Appointment appointment) {
        if (!properties.isEnabled()) {
            log.debug("Google Calendar integration is disabled. Skipping sync for appointment id: {}", appointment.getId());
            return;
        }

        try {
            Calendar client = getCalendarClient();
            if (client == null) {
                log.warn("Google Calendar client could not be initialized. Skipping sync.");
                return;
            }

            Event event = buildCalendarEvent(appointment);
            String calendarId = resolveCalendarId();

            Event resultEvent;
            if (appointment.getGoogleEventId() != null && !appointment.getGoogleEventId().isBlank()) {
                try {
                    resultEvent = client.events()
                            .update(calendarId, appointment.getGoogleEventId(), event)
                            .setSendUpdates(properties.isSendNotifications() ? "all" : "none")
                            .execute();
                    log.info("Google Calendar event updated successfully: {} (Appointment ID: {})", resultEvent.getId(), appointment.getId());
                } catch (GoogleJsonResponseException e) {
                    if (e.getStatusCode() == 404) {
                        log.warn("Existing Google Calendar event not found (404), creating new event for appointment ID: {}", appointment.getId());
                        resultEvent = client.events()
                                .insert(calendarId, event)
                                .setSendUpdates(properties.isSendNotifications() ? "all" : "none")
                                .execute();
                    } else {
                        throw e;
                    }
                }
            } else {
                resultEvent = client.events()
                        .insert(calendarId, event)
                        .setSendUpdates(properties.isSendNotifications() ? "all" : "none")
                        .execute();
                log.info("Google Calendar event created successfully: {} (Appointment ID: {})", resultEvent.getId(), appointment.getId());
            }

            if (resultEvent != null) {
                appointment.setGoogleEventId(resultEvent.getId());
                appointment.setGoogleEventLink(resultEvent.getHtmlLink());
            }
        } catch (Exception e) {
            log.error("Failed to sync appointment id {} with Google Calendar: {}", appointment.getId(), e.getMessage(), e);
        }
    }

    /**
     * Elimina el evento asociado de Google Calendar cuando una cita es cancelada.
     */
    public void cancelAppointmentEvent(Appointment appointment) {
        if (!properties.isEnabled() || appointment.getGoogleEventId() == null || appointment.getGoogleEventId().isBlank()) {
            return;
        }

        try {
            Calendar client = getCalendarClient();
            if (client == null) {
                return;
            }

            String calendarId = resolveCalendarId();
            client.events()
                    .delete(calendarId, appointment.getGoogleEventId())
                    .setSendUpdates(properties.isSendNotifications() ? "all" : "none")
                    .execute();
            log.info("Google Calendar event deleted: {} (Appointment ID: {})", appointment.getGoogleEventId(), appointment.getId());

            appointment.setGoogleEventId(null);
            appointment.setGoogleEventLink(null);
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.info("Google Calendar event was already deleted or not found: {}", appointment.getGoogleEventId());
                appointment.setGoogleEventId(null);
                appointment.setGoogleEventLink(null);
            } else {
                log.error("Failed to delete Google Calendar event for appointment id {}: {}", appointment.getId(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to delete Google Calendar event for appointment id {}: {}", appointment.getId(), e.getMessage(), e);
        }
    }

    protected Calendar getCalendarClient() {
        try {
            InputStream credentialsStream = resolveCredentialsStream();
            if (credentialsStream == null) {
                log.warn("No credentials provided for Google Calendar (neither credentialsPath nor credentialsJson).");
                return null;
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            ).setApplicationName("ClinicaPersonal").build();
        } catch (Exception e) {
            log.error("Error initializing Google Calendar client: {}", e.getMessage(), e);
            return null;
        }
    }

    InputStream resolveCredentialsStream() {
        // 1. Prioridad: JSON directo o Base64 desde variable de entorno
        if (properties.getCredentialsJson() != null && !properties.getCredentialsJson().isBlank()) {
            String rawJson = properties.getCredentialsJson().trim();
            if (!rawJson.startsWith("{")) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(rawJson);
                    return new ByteArrayInputStream(decoded);
                } catch (IllegalArgumentException e) {
                    log.warn("credentialsJson is not raw JSON nor valid Base64 string");
                }
            }
            return new ByteArrayInputStream(rawJson.getBytes(StandardCharsets.UTF_8));
        }

        // 2. Ruta a archivo en disco con resolución flexible de directorios
        String path = properties.getCredentialsPath();
        if (path != null && !path.isBlank()) {
            path = path.trim();
            File[] candidateFiles = new File[] {
                    new File(path),
                    new File(path.replaceFirst("^\\.?/?backend/", "")),
                    new File("backend", path.replaceFirst("^\\.?/?backend/", ""))
            };

            for (File file : candidateFiles) {
                if (file.exists() && file.isFile()) {
                    try {
                        log.info("Loading Google Calendar credentials from file: {}", file.getAbsolutePath());
                        return new FileInputStream(file);
                    } catch (Exception e) {
                        log.warn("Failed to open credentials file from candidate {}: {}", file.getAbsolutePath(), e.getMessage());
                    }
                }
            }

            try {
                InputStream classpathStream = getClass().getClassLoader().getResourceAsStream(path);
                if (classpathStream != null) {
                    log.info("Loading Google Calendar credentials from classpath: {}", path);
                    return classpathStream;
                }
            } catch (Exception e) {
                log.warn("Failed to open credentials from classpath {}: {}", path, e.getMessage());
            }

            log.warn("Credentials file does not exist at path: {} (checked multiple relative locations)", properties.getCredentialsPath());
        } else {
            // Intento de fallback automático si existe google-credentials.json en el directorio
            File[] fallbacks = new File[] {
                    new File("google-credentials.json"),
                    new File("backend/google-credentials.json")
            };
            for (File f : fallbacks) {
                if (f.exists() && f.isFile()) {
                    try {
                        log.info("Found fallback Google Calendar credentials file at: {}", f.getAbsolutePath());
                        return new FileInputStream(f);
                    } catch (Exception e) {
                        log.warn("Failed to open fallback credentials file: {}", e.getMessage());
                    }
                }
            }
        }

        return null;
    }

    private String resolveCalendarId() {
        String calId = properties.getCalendarId();
        return (calId != null && !calId.isBlank()) ? calId.trim() : "primary";
    }

    private Event buildCalendarEvent(Appointment appointment) {
        Event event = new Event();

        Patient patient = appointment.getPatient();
        String patientFullName = patient != null
                ? (patient.getFirstName() + " " + patient.getLastName()).trim()
                : "Paciente";

        String serviceName = appointment.getClinicalService() != null
                ? appointment.getClinicalService().getName()
                : appointment.getSpecialty();

        event.setSummary("Cita: " + patientFullName + " - " + serviceName);

        StringBuilder description = new StringBuilder();
        description.append("📋 Cita Médica Confirmada\n");
        description.append("• Paciente: ").append(patientFullName).append("\n");
        if (patient != null && patient.getContactNumber() != null && !patient.getContactNumber().isBlank()) {
            description.append("• Teléfono: ").append(patient.getContactNumber()).append("\n");
        }
        if (patient != null && patient.getEmail() != null && !patient.getEmail().isBlank()) {
            description.append("• Correo: ").append(patient.getEmail()).append("\n");
        }
        description.append("• Especialidad: ").append(appointment.getSpecialty()).append("\n");
        if (appointment.getClinicalService() != null) {
            description.append("• Servicio: ").append(appointment.getClinicalService().getName()).append("\n");
        }
        description.append("• Modalidad: ").append(appointment.getModality()).append("\n");

        if ("VIRTUAL".equalsIgnoreCase(appointment.getModality()) && appointment.getVideoCallLink() != null && !appointment.getVideoCallLink().isBlank()) {
            description.append("• Enlace Videollamada: ").append(appointment.getVideoCallLink()).append("\n");
            event.setLocation(appointment.getVideoCallLink());
        } else {
            event.setLocation("Consultorio Clínica");
        }

        if (appointment.getNotes() != null && !appointment.getNotes().isBlank()) {
            description.append("• Notas: ").append(appointment.getNotes()).append("\n");
        }

        event.setDescription(description.toString());

        // Fecha y hora
        String timezone = (properties.getTimezone() != null && !properties.getTimezone().isBlank())
                ? properties.getTimezone()
                : "America/Lima";
        ZoneId zoneId = ZoneId.of(timezone);

        LocalDate date = appointment.getAppointmentDate();
        LocalTime startTime = appointment.getStartTime() != null ? appointment.getStartTime() : LocalTime.of(9, 0);
        LocalTime endTime = appointment.getEndTime() != null ? appointment.getEndTime() : startTime.plusMinutes(30);

        ZonedDateTime startZoned = ZonedDateTime.of(date, startTime, zoneId);
        ZonedDateTime endZoned = ZonedDateTime.of(date, endTime, zoneId);

        DateTime startDateTime = new DateTime(Date.from(startZoned.toInstant()));
        DateTime endDateTime = new DateTime(Date.from(endZoned.toInstant()));

        event.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone(timezone));
        event.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone(timezone));

        return event;
    }
}
