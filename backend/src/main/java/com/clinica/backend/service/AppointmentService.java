package com.clinica.backend.service;

import com.clinica.backend.dto.AppointmentDto;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Attention;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Payment;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.AppointmentScheduleLockRepository;
import com.clinica.backend.repository.AttentionRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AttentionRepository attentionRepository;
    private final PatientRepository patientRepository;
    private final ClinicalServiceRepository clinicalServiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final GoogleCalendarService googleCalendarService;
    private final ProfessionalScheduleService professionalScheduleService;
    private final ScheduleBlockService scheduleBlockService;
    private final AppointmentScheduleLockRepository appointmentScheduleLockRepository;

    private String getCurrentUserSpecialty() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSpecialty();
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDto> getByPatientId(Long patientId, Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        return mapPageToDto(appointmentRepository.findByPatientIdAndSpecialtyOrderByAppointmentDateDescStartTimeDesc(patientId, specialty, pageable));
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDto> getAll(Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        return mapPageToDto(appointmentRepository.findAllBySpecialtyOrderByAppointmentDateAscStartTimeAsc(specialty, pageable));
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDto> searchAppointments(String searchTerm, String status, Long professionalId, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        if (searchTerm == null)
            searchTerm = "";
        if (status != null && status.trim().isEmpty())
            status = null;
        String specialty = getCurrentUserSpecialty();

        return mapPageToDto(appointmentRepository.searchAppointmentsBySpecialty(searchTerm, status, professionalId, startDate, endDate, specialty, pageable));
    }

    @Transactional
    public AppointmentDto create(AppointmentDto dto) {
        String specialty = getCurrentUserSpecialty();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        ClinicalService clinicalService = resolveClinicalService(dto.getClinicalServiceId());

        int count = (dto.getRecurrenceCount() != null && dto.getRecurrenceCount() > 1)
                ? Math.min(dto.getRecurrenceCount(), 52)
                : 1;

        validateTimeRange(dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime());
        List<LocalDate> occurrenceDates = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            occurrenceDates.add(dto.getAppointmentDate().plusWeeks(i));
        }
        acquireScheduleLocks(specialty, occurrenceDates);

        // Validar todas las ocurrencias antes de persistir
        for (LocalDate occurrenceDate : occurrenceDates) {
            validateAppointmentTime(occurrenceDate, dto.getStartTime(), dto.getEndTime(), dto.getProfessionalId(), null);
            professionalScheduleService.validateProfessionalAvailability(dto.getProfessionalId(), occurrenceDate, dto.getStartTime(), dto.getEndTime(), specialty);
            scheduleBlockService.validateNoBlockConflict(occurrenceDate, dto.getStartTime(), dto.getEndTime(), dto.getProfessionalId(), specialty);
        }

        String recurrenceGroupId = count > 1 ? UUID.randomUUID().toString() : null;
        String recurrenceRule = count > 1 ? "WEEKLY;COUNT=" + count : null;

        Appointment firstSaved = null;
        for (int i = 0; i < count; i++) {
            LocalDate occurrenceDate = dto.getAppointmentDate().plusWeeks(i);
            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            appointment.setAppointmentDate(occurrenceDate);
            appointment.setStartTime(dto.getStartTime());
            appointment.setEndTime(dto.getEndTime());
            appointment.setStatus(dto.getStatus() == null ? "PROGRAMADA" : dto.getStatus());
            appointment.setModality(dto.getModality());
            appointment.setVideoCallLink(dto.getVideoCallLink());
            appointment.setProfessionalId(dto.getProfessionalId());
            appointment.setFirstTime(i == 0 && dto.isFirstTime());
            appointment.setClinicalSessionId(i == 0 ? dto.getClinicalSessionId() : null);
            appointment.setClinicalService(clinicalService);
            appointment.setNotes(dto.getNotes());
            appointment.setSpecialty(specialty);
            appointment.setConfirmationToken(UUID.randomUUID().toString());
            appointment.setRecurrenceGroupId(recurrenceGroupId);
            appointment.setRecurrenceRule(recurrenceRule);

            Appointment saved = appointmentRepository.save(appointment);
            if ("CONFIRMADA".equals(saved.getStatus())) {
                googleCalendarService.syncAppointment(saved);
                syncAttentionOnConfirmed(saved);
                saved = appointmentRepository.save(saved);
            }

            if (i == 0) {
                firstSaved = saved;
            }
        }

        return mapToDto(firstSaved);
    }

    @Transactional
    public AppointmentDto update(Long id, AppointmentDto dto) {
        return update(id, dto, false);
    }

    @Transactional
    public AppointmentDto update(Long id, AppointmentDto dto, boolean updateSeries) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));
        String specialty = getCurrentUserSpecialty();
        if (!specialty.equals(appointment.getSpecialty())) {
            throw new AccessDeniedException("Cita fuera de la especialidad del usuario");
        }

        ClinicalService clinicalService = resolveClinicalService(dto.getClinicalServiceId());

        if (updateSeries && appointment.getRecurrenceGroupId() != null) {
            List<Appointment> series = appointmentRepository
                    .findByRecurrenceGroupIdAndSpecialtyAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
                            appointment.getRecurrenceGroupId(), specialty, appointment.getAppointmentDate());

            validateTimeRange(appointment.getAppointmentDate(), dto.getStartTime(), dto.getEndTime());
            acquireScheduleLocks(specialty, series.stream().map(Appointment::getAppointmentDate).toList());

            for (Appointment app : series) {
                validateAppointmentTime(app.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), dto.getProfessionalId(), app.getId());
                professionalScheduleService.validateProfessionalAvailability(dto.getProfessionalId(), app.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), specialty);
                scheduleBlockService.validateNoBlockConflict(app.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), dto.getProfessionalId(), specialty);
            }

            for (Appointment app : series) {
                app.setStartTime(dto.getStartTime());
                app.setEndTime(dto.getEndTime());
                app.setModality(dto.getModality());
                app.setVideoCallLink(dto.getVideoCallLink());
                app.setProfessionalId(dto.getProfessionalId());
                app.setClinicalService(clinicalService);
                app.setNotes(dto.getNotes());
                if ("CONFIRMADA".equals(app.getStatus())) {
                    googleCalendarService.syncAppointment(app);
                }
                syncAttentionOnRescheduled(app);
                appointmentRepository.save(app);
            }
            return mapToDto(appointment);
        } else {
            validateTimeRange(dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime());
            acquireScheduleLocks(specialty, List.of(appointment.getAppointmentDate(), dto.getAppointmentDate()));
            validateAppointmentTime(dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), dto.getProfessionalId(), id);
            professionalScheduleService.validateProfessionalAvailability(dto.getProfessionalId(), dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), specialty);
            scheduleBlockService.validateNoBlockConflict(dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), dto.getProfessionalId(), specialty);

            appointment.setAppointmentDate(dto.getAppointmentDate());
            appointment.setStartTime(dto.getStartTime());
            appointment.setEndTime(dto.getEndTime());
            appointment.setModality(dto.getModality());
            appointment.setVideoCallLink(dto.getVideoCallLink());
            appointment.setProfessionalId(dto.getProfessionalId());
            appointment.setFirstTime(dto.isFirstTime());
            appointment.setClinicalService(clinicalService);
            appointment.setNotes(dto.getNotes());

            if ("CONFIRMADA".equals(appointment.getStatus())) {
                googleCalendarService.syncAppointment(appointment);
            }
            syncAttentionOnRescheduled(appointment);

            return mapToDto(appointmentRepository.save(appointment));
        }
    }

    @Transactional
    public AppointmentDto updateStatus(Long id, String newStatus) {
        return updateStatus(id, newStatus, false);
    }

    @Transactional
    public AppointmentDto updateStatus(Long id, String newStatus, boolean updateSeries) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));
        String specialty = getCurrentUserSpecialty();
        if (!specialty.equals(appointment.getSpecialty())) {
            throw new AccessDeniedException("Cita fuera de la especialidad del usuario");
        }

        if (updateSeries && appointment.getRecurrenceGroupId() != null) {
            List<Appointment> series = appointmentRepository
                    .findByRecurrenceGroupIdAndSpecialtyAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
                            appointment.getRecurrenceGroupId(), specialty, appointment.getAppointmentDate());
            for (Appointment app : series) {
                applyStatusTransition(app, newStatus);
                appointmentRepository.save(app);
            }
            return mapToDto(appointment);
        } else {
            applyStatusTransition(appointment, newStatus);
            return mapToDto(appointmentRepository.save(appointment));
        }
    }

    private void applyStatusTransition(Appointment appointment, String newStatus) {
        String currentStatus = appointment.getStatus();
        if (newStatus.equals(currentStatus)) {
            return;
        }

        // Máquina de estados
        if ("PROGRAMADA".equals(currentStatus)) {
            if (!"CONFIRMADA".equals(newStatus) && !"CANCELADA".equals(newStatus)) {
                throw new IllegalArgumentException("Transición no válida desde PROGRAMADA");
            }
        } else if ("CONFIRMADA".equals(currentStatus)) {
            if (!"COMPLETADA".equals(newStatus) && !"CANCELADA".equals(newStatus) && !"NO_ASISTIO".equals(newStatus)) {
                throw new IllegalArgumentException("Transición no válida desde CONFIRMADA");
            }
        } else if ("COMPLETADA".equals(currentStatus) || "CANCELADA".equals(currentStatus)
                || "NO_ASISTIO".equals(currentStatus)) {
            throw new IllegalArgumentException("La cita está en un estado final y no puede cambiar.");
        }

        appointment.setStatus(newStatus);

        if ("CONFIRMADA".equals(newStatus)) {
            googleCalendarService.syncAppointment(appointment);
            syncAttentionOnConfirmed(appointment);
        } else if ("CANCELADA".equals(newStatus) || "NO_ASISTIO".equals(newStatus)) {
            if ("CANCELADA".equals(newStatus)) {
                googleCalendarService.cancelAppointmentEvent(appointment);
            }
            syncAttentionOnCancelled(appointment);
        }
    }

    private void syncAttentionOnConfirmed(Appointment appointment) {
        if (attentionRepository == null || appointment == null || appointment.getId() == null) {
            return;
        }
        var existing = attentionRepository.findByAppointmentIdAndDeletedFalse(appointment.getId());
        if (existing.isEmpty()) {
            User professional = null;
            if (appointment.getProfessionalId() != null && userRepository != null) {
                professional = userRepository.findById(appointment.getProfessionalId()).orElse(null);
            }
            if (professional == null) {
                try {
                    professional = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                } catch (Exception ignored) {
                }
            }

            Attention attention = new Attention();
            attention.setPatient(appointment.getPatient());
            attention.setProfessional(professional);
            attention.setSpecialty(appointment.getSpecialty());
            attention.setAttentionDate(appointment.getAppointmentDate());
            attention.setStartTime(appointment.getStartTime());
            attention.setEndTime(appointment.getEndTime());
            attention.setAppointment(appointment);
            attention.setClinicalService(appointment.getClinicalService());
            attention.setMotive(appointment.getNotes());
            attention.setStatus(Attention.STATUS_AGENDADA);

            Attention saved = attentionRepository.save(attention);
            if (saved != null) {
                appointment.setAttentionId(saved.getId());
            }
        }
    }

    private void syncAttentionOnCancelled(Appointment appointment) {
        if (attentionRepository == null || appointment == null || appointment.getId() == null) {
            return;
        }
        var existing = attentionRepository.findByAppointmentIdAndDeletedFalse(appointment.getId());
        if (existing.isPresent()) {
            Attention att = existing.get();
            if (Attention.STATUS_AGENDADA.equals(att.getStatus())) {
                att.setStatus(Attention.STATUS_CANCELADA);
                attentionRepository.save(att);
            }
        }
    }

    private void syncAttentionOnRescheduled(Appointment appointment) {
        if (attentionRepository == null || appointment == null || appointment.getId() == null) {
            return;
        }
        var existing = attentionRepository.findByAppointmentIdAndDeletedFalse(appointment.getId());
        if (existing.isPresent()) {
            Attention att = existing.get();
            if (Attention.STATUS_AGENDADA.equals(att.getStatus())) {
                att.setAttentionDate(appointment.getAppointmentDate());
                att.setStartTime(appointment.getStartTime());
                att.setEndTime(appointment.getEndTime());
                att.setClinicalService(appointment.getClinicalService());
                att.setMotive(appointment.getNotes());
                attentionRepository.save(att);
            }
        }
    }

    private void validateAppointmentTime(LocalDate date, LocalTime start, LocalTime end, Long professionalId, Long excludeId) {
        validateTimeRange(date, start, end);

        String specialty = getCurrentUserSpecialty();
        List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(date, start, end, specialty, professionalId);

        boolean hasConflict = overlapping.stream()
                .anyMatch(app -> excludeId == null || !app.getId().equals(excludeId));

        if (hasConflict) {
            throw new ConflictException("Ya existe una cita programada en este horario (" + date + " " + start + " - " + end + ").");
        }
    }

    private void validateTimeRange(LocalDate date, LocalTime start, LocalTime end) {
        if (date == null || start == null || end == null) {
            throw new IllegalArgumentException("La fecha, hora de inicio y hora de fin son obligatorias.");
        }

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("La hora de inicio (" + start + ") debe ser anterior a la hora de fin (" + end + ").");
        }
    }

    private void acquireScheduleLocks(String specialty, Collection<LocalDate> dates) {
        for (LocalDate date : new TreeSet<>(dates)) {
            appointmentScheduleLockRepository.acquireScheduleLock(specialty, date);
        }
    }

    private ClinicalService resolveClinicalService(Long clinicalServiceId) {
        if (clinicalServiceId == null) {
            return null;
        }
        return clinicalServiceRepository.findByIdAndSpecialtyAndDeletedFalse(clinicalServiceId, getCurrentUserSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio clínico no encontrado"));
    }

    private AppointmentDto mapToDto(Appointment appointment) {
        Payment payment = appointment.getId() != null
                ? paymentRepository.findFirstByAppointmentIdAndDeletedFalse(appointment.getId()).orElse(null)
                : null;
        return mapToDto(appointment, payment);
    }

    private AppointmentDto mapToDto(Appointment appointment, Payment payment) {
        String professionalName = null;
        if (appointment.getProfessionalId() != null) {
            professionalName = userRepository.findById(appointment.getProfessionalId())
                    .map(User::getFullName)
                    .orElse(null);
        }
        return mapToDto(appointment, payment, professionalName);
    }

    private AppointmentDto mapToDto(Appointment appointment, Payment payment, String professionalName) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(appointment.getId());
        dto.setPatientId(appointment.getPatient().getId());
        dto.setPatientUuid(appointment.getPatient().getUuid() != null ? appointment.getPatient().getUuid().toString() : null);
        dto.setPatientName(appointment.getPatient().getFullName());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setStartTime(appointment.getStartTime());
        dto.setEndTime(appointment.getEndTime());
        dto.setStatus(appointment.getStatus());
        dto.setModality(appointment.getModality());
        dto.setVideoCallLink(appointment.getVideoCallLink());
        dto.setProfessionalId(appointment.getProfessionalId());
        dto.setProfessionalName(professionalName);
        dto.setFirstTime(appointment.isFirstTime());
        dto.setClinicalSessionId(appointment.getClinicalSessionId());
        dto.setAttentionId(appointment.getAttentionId());
        if (appointment.getClinicalService() != null) {
            dto.setClinicalServiceId(appointment.getClinicalService().getId());
            dto.setClinicalServiceName(appointment.getClinicalService().getName());
        }
        dto.setNotes(appointment.getNotes());
        dto.setSpecialty(appointment.getSpecialty());
        dto.setGoogleEventId(appointment.getGoogleEventId());
        dto.setGoogleEventLink(appointment.getGoogleEventLink());
        dto.setReminderSentAt(appointment.getReminderSentAt());
        dto.setConfirmedAt(appointment.getConfirmedAt());
        dto.setRecurrenceGroupId(appointment.getRecurrenceGroupId());
        dto.setRecurrenceRule(appointment.getRecurrenceRule());
        if (appointment.getPatient() != null) {
            dto.setPatientEmail(appointment.getPatient().getEmail());
            dto.setPatientPhone(appointment.getPatient().getContactNumber());
        }

        if (payment != null) {
            dto.setPaid(true);
            dto.setPaymentId(payment.getId());
            dto.setPaymentAmount(payment.getAmount());
        } else {
            dto.setPaid(false);
        }
        return dto;
    }

    private Page<AppointmentDto> mapPageToDto(Page<Appointment> page) {
        List<Long> appointmentIds = page.getContent().stream()
                .map(Appointment::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        Map<Long, Payment> paymentsByAppointmentId = new HashMap<>();
        if (!appointmentIds.isEmpty()) {
            paymentRepository.findByAppointmentIdInAndDeletedFalse(appointmentIds)
                    .forEach(p -> {
                        if (p.getAppointment() != null && p.getAppointment().getId() != null) {
                            paymentsByAppointmentId.putIfAbsent(p.getAppointment().getId(), p);
                        }
                    });
        }

        var professionalIds = page.getContent().stream()
                .map(Appointment::getProfessionalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> professionalNamesById = professionalIds.isEmpty()
                ? Map.of()
                : userRepository.findByIdIn(professionalIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getFullName));

        return page.map(app -> mapToDto(
                app,
                paymentsByAppointmentId.get(app.getId()),
                app.getProfessionalId() == null
                        ? null
                        : professionalNamesById.get(app.getProfessionalId())));
    }
}
