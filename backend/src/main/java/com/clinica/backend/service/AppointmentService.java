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
}
