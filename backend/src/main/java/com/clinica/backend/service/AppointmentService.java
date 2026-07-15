package com.clinica.backend.service;
import com.clinica.backend.dto.AppointmentDto;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    public List<AppointmentDto> getByPatientId(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patientId).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }
    public List<AppointmentDto> getAll() {
        return appointmentRepository.findAllByOrderByAppointmentDateAsc().stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }
    public AppointmentDto create(AppointmentDto dto) {
        validateAppointmentTime(dto.getAppointmentDate(), null);
        Patient patient = patientRepository.findById(dto.getPatientId()).orElseThrow();
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setStatus(dto.getStatus());
        appointment.setNotes(dto.getNotes());
        return mapToDto(appointmentRepository.save(appointment));
    }
    private AppointmentDto mapToDto(Appointment appointment) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(appointment.getId());
        dto.setPatientId(appointment.getPatient().getId());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setStatus(appointment.getStatus());
        dto.setNotes(appointment.getNotes());
        return dto;
    }

    public AppointmentDto update(Long id, AppointmentDto dto) {
        validateAppointmentTime(dto.getAppointmentDate(), id);
        Appointment appointment = appointmentRepository.findById(id).orElseThrow();
        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setNotes(dto.getNotes());
        // Do not update patient or status here, those have their own flow
        return mapToDto(appointmentRepository.save(appointment));
    }

    public AppointmentDto updateStatus(Long id, String status) {
        Appointment appointment = appointmentRepository.findById(id).orElseThrow();
        appointment.setStatus(status);
        return mapToDto(appointmentRepository.save(appointment));
    }

    private void validateAppointmentTime(LocalDateTime newTime, Long excludeId) {
        if (newTime == null) return;
        LocalDateTime start = newTime.minusMinutes(29);
        LocalDateTime end = newTime.plusMinutes(29);
        List<Appointment> overlapping = appointmentRepository.findByAppointmentDateBetweenAndStatusNot(start, end, "CANCELLED");
        
        boolean hasConflict = overlapping.stream()
                .anyMatch(app -> excludeId == null || !app.getId().equals(excludeId));
                
        if (hasConflict) {
            throw new IllegalArgumentException("Ya existe una cita programada en este horario o con menos de 30 minutos de diferencia.");
        }
    }
}
