package com.clinica.backend.service;

import com.clinica.backend.model.Appointment;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String CLINIC_DEFAULT_NAME = "Clínica";

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;
    private final ClinicSettingsRepository clinicSettingsRepository;

    @Value("${appointment.reminder-lead-days:1}")
    private int reminderLeadDays;

    @Value("${appointment.public-base-url:http://localhost:4200}")
    private String publicBaseUrl;

    @Scheduled(cron = "${appointment.reminder-cron:0 30 9 * * *}")
    @Transactional
    public void sendDueReminders() {
        if (!emailService.isConfigured()) {
            log.warn("Recordatorios de cita omitidos: SMTP no configurado.");
            return;
        }

        LocalDate targetDate = LocalDate.now().plusDays(reminderLeadDays);
        List<Appointment> appointments = appointmentRepository.findPendingRemindersForDate(targetDate);
        int sent = 0;

        for (Appointment appointment : appointments) {
            String email = appointment.getPatient() != null ? appointment.getPatient().getEmail() : null;
            if (email == null || email.isBlank()) {
                continue;
            }
            if (appointment.getConfirmationToken() == null || appointment.getConfirmationToken().isBlank()) {
                continue;
            }

            boolean delivered = emailService.sendHtml(
                    email,
                    "Recordatorio de su cita",
                    buildReminderHtml(appointment)
            );
            if (delivered) {
                appointment.setReminderSentAt(LocalDateTime.now());
                appointmentRepository.save(appointment);
                sent++;
            }
        }

        log.info("Recordatorios enviados para {} citas del día {}", sent, targetDate);
    }

    private String buildReminderHtml(Appointment appointment) {
        String clinicName = clinicSettingsRepository
                .findTopBySpecialtyAndDeletedFalseOrderByIdAsc(appointment.getSpecialty())
                .map(settings -> settings.getClinicName() != null ? settings.getClinicName() : CLINIC_DEFAULT_NAME)
                .orElse(CLINIC_DEFAULT_NAME);

        String patientName = appointment.getPatient() != null
                ? (appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName()).trim()
                : "Paciente";
        String time = appointment.getStartTime() != null ? appointment.getStartTime().toString() : "";
        String confirmationLink = publicBaseUrl + "/confirmar-cita/" + appointment.getConfirmationToken();

        return "<html><body style=\"font-family: Arial, sans-serif; color: #1e293b;\">"
                + "<h2 style=\"color: #047857;\">" + clinicName + "</h2>"
                + "<p>Hola <strong>" + patientName + "</strong>,</p>"
                + "<p>Le recordamos que tiene una cita programada para el "
                + "<strong>" + appointment.getAppointmentDate().format(DATE_FORMATTER) + "</strong>"
                + (time.isBlank() ? "" : " a las <strong>" + time + "</strong>") + ".</p>"
                + "<p>Por favor, confirme su asistencia haciendo clic en el siguiente enlace:</p>"
                + "<p><a href=\"" + confirmationLink + "\" style=\"background-color:#047857;color:#ffffff;padding:10px 16px;text-decoration:none;border-radius:6px;\">Confirmar mi cita</a></p>"
                + "<p style=\"color:#64748b;font-size:12px;\">Si no puede asistir, le agradeceremos comunicarse con nosotros.</p>"
                + "</body></html>";
    }
}
