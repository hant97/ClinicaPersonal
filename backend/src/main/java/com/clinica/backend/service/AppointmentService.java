package com.clinica.backend.service;

import com.clinica.backend.dto.AppointmentDto;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public Page<AppointmentDto> getByPatientId(Long patientId, Pageable pageable) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(patientId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDto> getAll(Pageable pageable) {
        return appointmentRepository.findAllByOrderByAppointmentDateAscStartTimeAsc(pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDto> searchAppointments(String searchTerm, String status, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        if (searchTerm == null)
            searchTerm = "";
        if (status != null && status.trim().isEmpty())
            status = null;

        return appointmentRepository.searchAppointments(searchTerm, status, startDate, endDate, pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public AppointmentDto create(AppointmentDto dto) {
        validateAppointmentTime(dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), null);
        Patient patient = patientRepository.findById(dto.getPatientId()).orElseThrow();
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

        return mapToDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDto update(Long id, AppointmentDto dto) {
        validateAppointmentTime(dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), id);
        Appointment appointment = appointmentRepository.findById(id).orElseThrow();

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

    private void validateAppointmentTime(LocalDate date, LocalTime start, LocalTime end, Long excludeId) {
        if (date == null || start == null || end == null)
            return;

        List<Appointment> overlapping = appointmentRepository.findByAppointmentDateAndStatusNot(date, "CANCELADA");

        boolean hasConflict = overlapping.stream()
                .filter(app -> excludeId == null || !app.getId().equals(excludeId))
                .anyMatch(app -> {
                    // Conflicto si el inicio propuesto está estrictamente antes del fin existente
                    // Y el fin propuesto está estrictamente después del inicio existente
                    return start.isBefore(app.getEndTime()) && end.isAfter(app.getStartTime());
                });

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
        return dto;
    }
}
