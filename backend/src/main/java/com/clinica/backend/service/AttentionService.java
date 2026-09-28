package com.clinica.backend.service;

import com.clinica.backend.dto.AttentionDto;
import com.clinica.backend.dto.AttentionSummaryDto;
import com.clinica.backend.dto.ProfessionalProductivityDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.mapper.AttentionMapper;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Attention;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.AttentionRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttentionService {

    private final AttentionRepository attentionRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicalSessionRepository clinicalSessionRepository;
    private final AttentionReportService attentionReportService;
    private final AttentionStatusService attentionStatusService;
    private final AttentionLinkService attentionLinkService;
    private final ClinicalServiceRepository clinicalServiceRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final AuditLogService auditLogService;
    private final AttentionMapper attentionMapper;

    @Transactional(readOnly = true)
    public Page<AttentionDto> searchAttentions(
            String searchTerm,
            String status,
            Long professionalId,
            Long patientId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();

        String cleanSearchTerm = (searchTerm != null && !searchTerm.trim().isEmpty()) ? searchTerm.trim() : null;
        String cleanStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim() : null;

        Page<Attention> page = attentionRepository.searchAttentions(
                specialty,
                patientId,
                professionalId,
                cleanStatus,
                startDate,
                endDate,
                cleanSearchTerm,
                pageable
        );

        return page.map(attentionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public AttentionDto getAttentionById(Long id) {
        User user = clinicalAuthorizationService.currentUser();
        Attention attention = attentionRepository.findByIdAndSpecialtyAndDeletedFalse(id, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + id));
        return attentionMapper.toDto(attention);
    }

    public AttentionSummaryDto getTodaySummary() {
        return attentionReportService.getTodaySummary();
    }

    @Transactional
    public AttentionDto create(AttentionDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();

        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + dto.getPatientId()));

        User professional = ProfessionalAssignmentValidator.resolve(dto.getProfessionalId(), user, userRepository);

        Attention attention = new Attention();
        attention.setPatient(patient);
        attention.setProfessional(professional);
        attention.setSpecialty(specialty);
        attention.setAttentionDate(dto.getAttentionDate() != null ? dto.getAttentionDate() : LocalDate.now());
        attention.setMotive(dto.getMotive());
        attention.setNotes(dto.getNotes());

        String initialStatus = dto.getStatus() != null && !dto.getStatus().isBlank()
                ? dto.getStatus()
                : Attention.STATUS_AGENDADA;
        attention.setStatus(initialStatus);

        if (dto.getStartTime() != null) {
            attention.setStartTime(dto.getStartTime());
        } else if (Attention.STATUS_EN_PROCESO.equals(initialStatus)) {
            attention.setStartTime(LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
        }

        if (dto.getEndTime() != null) {
            attention.setEndTime(dto.getEndTime());
        }

        if (dto.getDurationMinutes() != null) {
            attention.setDurationMinutes(dto.getDurationMinutes());
        } else if (attention.getStartTime() != null && attention.getEndTime() != null) {
            attention.setDurationMinutes((int) ChronoUnit.MINUTES.between(attention.getStartTime(), attention.getEndTime()));
        }

        if (dto.getClinicalServiceId() != null) {
            clinicalServiceRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getClinicalServiceId(), specialty)
                    .ifPresent(attention::setClinicalService);
        }

        if (dto.getAppointmentId() != null) {
            appointmentRepository.findByIdAndSpecialty(dto.getAppointmentId(), specialty)
                    .ifPresent(app -> {
                        attention.setAppointment(app);
                        app.setAttentionId(attention.getId());
                        if (Attention.STATUS_EN_PROCESO.equals(initialStatus)) {
                            app.setStatus("CONFIRMADA");
                        } else if (Attention.STATUS_ATENDIDA.equals(initialStatus) || Attention.STATUS_COBRADA.equals(initialStatus)) {
                            app.setStatus("COMPLETADA");
                        }
                    });
        }

        Attention saved = attentionRepository.save(attention);

        if (saved.getAppointment() != null) {
            saved.getAppointment().setAttentionId(saved.getId());
            appointmentRepository.save(saved.getAppointment());
        }

        auditLogService.record(
                "CREATE",
                "ATTENTION",
                saved.getId().toString(),
                "Atención creada (Estado: " + saved.getStatus() + ", Fecha: " + saved.getAttentionDate() + ") para paciente ID: " + patient.getId()
        );

        return attentionMapper.toDto(saved);
    }

    @Transactional
    public AttentionDto createFromAppointment(Long appointmentId) {
        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();

        Appointment appointment = appointmentRepository.findByIdAndSpecialty(appointmentId, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + appointmentId));

        // Si ya existe una atención activa vinculada a esta cita, retornarla
        var existing = attentionRepository.findByAppointmentIdAndDeletedFalse(appointmentId);
        if (existing.isPresent()) {
            Attention att = existing.get();
            if (Attention.STATUS_AGENDADA.equals(att.getStatus())) {
                att.setStatus(Attention.STATUS_EN_PROCESO);
                if (att.getStartTime() == null) {
                    att.setStartTime(LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
                }
                attentionRepository.save(att);
            }
            return attentionMapper.toDto(att);
        }

        User professional = ProfessionalAssignmentValidator.resolve(appointment.getProfessionalId(), user, userRepository);

        Attention attention = new Attention();
        attention.setPatient(appointment.getPatient());
        attention.setProfessional(professional);
        attention.setSpecialty(specialty);
        attention.setAttentionDate(appointment.getAppointmentDate());
        attention.setStartTime(appointment.getStartTime() != null ? appointment.getStartTime() : LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
        attention.setAppointment(appointment);
        attention.setClinicalService(appointment.getClinicalService());
        attention.setMotive(appointment.getNotes());
        attention.setStatus(Attention.STATUS_EN_PROCESO);

        if (appointment.getClinicalSessionId() != null) {
            clinicalSessionRepository.findByIdAndDeletedFalse(appointment.getClinicalSessionId())
                    .ifPresent(attention::setClinicalSession);
        }

        Attention saved = attentionRepository.save(attention);

        appointment.setAttentionId(saved.getId());
        appointmentRepository.save(appointment);

        auditLogService.record(
                "CREATE",
                "ATTENTION",
                saved.getId().toString(),
                "Atención iniciada desde Cita ID: " + appointmentId + " para paciente ID: " + appointment.getPatient().getId()
        );

        return attentionMapper.toDto(saved);
    }

    @Transactional
    public AttentionDto update(Long id, AttentionDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();

        Attention attention = attentionRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + id));

        if (dto.getMotive() != null) {
            attention.setMotive(dto.getMotive());
        }
        if (dto.getNotes() != null) {
            attention.setNotes(dto.getNotes());
        }
        if (dto.getStartTime() != null) {
            attention.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            attention.setEndTime(dto.getEndTime());
        }
        if (dto.getDurationMinutes() != null) {
            attention.setDurationMinutes(dto.getDurationMinutes());
        } else if (attention.getStartTime() != null && attention.getEndTime() != null) {
            attention.setDurationMinutes((int) ChronoUnit.MINUTES.between(attention.getStartTime(), attention.getEndTime()));
        }

        if (dto.getClinicalServiceId() != null) {
            clinicalServiceRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getClinicalServiceId(), specialty)
                    .ifPresent(attention::setClinicalService);
        }

        Attention updated = attentionRepository.save(attention);

        auditLogService.record(
                "UPDATE",
                "ATTENTION",
                updated.getId().toString(),
                "Atención actualizada ID: " + updated.getId()
        );

        return attentionMapper.toDto(updated);
    }

    public AttentionDto updateStatus(Long id, String newStatus, String notes) {
        return attentionStatusService.updateStatus(id, newStatus, notes);
    }

    public AttentionDto linkClinicalSession(Long attentionId, Long sessionId) {
        return attentionLinkService.linkClinicalSession(attentionId, sessionId);
    }

    public AttentionDto linkPrescription(Long attentionId, Long prescriptionId) {
        return attentionLinkService.linkPrescription(attentionId, prescriptionId);
    }

    public AttentionDto linkPayment(Long attentionId, Long paymentId) {
        return attentionLinkService.linkPayment(attentionId, paymentId);
    }

    @Transactional
    public void delete(Long id) {
        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();

        Attention attention = attentionRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + id));

        attention.setDeleted(true);
        attention.setDeletedAt(LocalDateTime.now());
        attention.setDeletedBy(user.getId());
        attentionRepository.save(attention);

        auditLogService.record(
                "DELETE",
                "ATTENTION",
                id.toString(),
                "Atención eliminada lógicamente ID: " + id
        );
    }


    public List<ProfessionalProductivityDto> getProfessionalProductivity(LocalDate dateFrom, LocalDate dateTo) {
        return attentionReportService.getProfessionalProductivity(dateFrom, dateTo);
    }

}
