package com.clinica.backend.service;

import com.clinica.backend.dto.AppointmentDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    private String getCurrentUserSpecialty() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSpecialty();
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDto> getByPatientId(Long patientId, Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        return appointmentRepository.findByPatientIdAndSpecialtyOrderByAppointmentDateDescStartTimeDesc(patientId, specialty, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDto> getAll(Pageable pageable) {
        String specialty = getCurrentUserSpecialty();
        return appointmentRepository.findAllBySpecialtyOrderByAppointmentDateAscStartTimeAsc(specialty, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDto> searchAppointments(String searchTerm, String status, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        if (searchTerm == null)
            searchTerm = "";
        if (status != null && status.trim().isEmpty())
            status = null;
        String specialty = getCurrentUserSpecialty();

        return appointmentRepository.searchAppointmentsBySpecialty(searchTerm, status, startDate, endDate, specialty, pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public AppointmentDto create(AppointmentDto dto) {
        validateAppointmentTime(dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), dto.getProfessionalId(), null);
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), getCurrentUserSpecialty()).orElseThrow();
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);

        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setStartTime(dto.getStartTime());
        appointment.setEndTime(dto.getEndTime());
        appointment.setStatus(dto.getStatus() == null ? "PROGRAMADA" : dto.getStatus());
        appointment.setModality(dto.getModality());
        appointment.setVideoCallLink(dto.getVideoCallLink());
        appointment.setProfessionalId(dto.getProfessionalId());
        appointment.setFirstTime(dto.isFirstTime());
        appointment.setClinicalSessionId(dto.getClinicalSessionId());
        appointment.setNotes(dto.getNotes());
        appointment.setSpecialty(getCurrentUserSpecialty());

        return mapToDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDto update(Long id, AppointmentDto dto) {
        validateAppointmentTime(dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), dto.getProfessionalId(), id);
        Appointment appointment = appointmentRepository.findById(id).orElseThrow();
        if (!getCurrentUserSpecialty().equals(appointment.getSpecialty())) {
            throw new AccessDeniedException("Cita fuera de la especialidad del usuario");
        }

        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setStartTime(dto.getStartTime());
        appointment.setEndTime(dto.getEndTime());
        appointment.setModality(dto.getModality());
        appointment.setVideoCallLink(dto.getVideoCallLink());
        appointment.setProfessionalId(dto.getProfessionalId());
        appointment.setFirstTime(dto.isFirstTime());
        appointment.setNotes(dto.getNotes());

        return mapToDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDto updateStatus(Long id, String newStatus) {
        Appointment appointment = appointmentRepository.findById(id).orElseThrow();
        if (!getCurrentUserSpecialty().equals(appointment.getSpecialty())) {
            throw new AccessDeniedException("Cita fuera de la especialidad del usuario");
        }
        String currentStatus = appointment.getStatus();

        if (newStatus.equals(currentStatus)) {
            return mapToDto(appointment);
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
        return mapToDto(appointmentRepository.save(appointment));
    }

    private void validateAppointmentTime(LocalDate date, LocalTime start, LocalTime end, Long professionalId, Long excludeId) {
        if (date == null || start == null || end == null) {
            throw new IllegalArgumentException("La fecha, hora de inicio y hora de fin son obligatorias.");
        }

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("La hora de inicio (" + start + ") debe ser anterior a la hora de fin (" + end + ").");
        }

        String specialty = getCurrentUserSpecialty();
        List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(date, start, end, specialty, professionalId);

        boolean hasConflict = overlapping.stream()
                .anyMatch(app -> excludeId == null || !app.getId().equals(excludeId));

        if (hasConflict) {
            throw new IllegalArgumentException("Ya existe una cita programada en este horario.");
        }
    }

    private AppointmentDto mapToDto(Appointment appointment) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(appointment.getId());
        dto.setPatientId(appointment.getPatient().getId());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setStartTime(appointment.getStartTime());
        dto.setEndTime(appointment.getEndTime());
        dto.setStatus(appointment.getStatus());
        dto.setModality(appointment.getModality());
        dto.setVideoCallLink(appointment.getVideoCallLink());
        dto.setProfessionalId(appointment.getProfessionalId());
        dto.setFirstTime(appointment.isFirstTime());
        dto.setClinicalSessionId(appointment.getClinicalSessionId());
        dto.setNotes(appointment.getNotes());
        dto.setSpecialty(appointment.getSpecialty());
        return dto;
    }
}
