package com.clinica.backend.service;

import com.clinica.backend.dto.AttentionDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.mapper.AttentionMapper;
import com.clinica.backend.model.Attention;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.AttentionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AttentionStatusService {
    private final AttentionRepository attentionRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final AuditLogService auditLogService;
    private final AttentionMapper attentionMapper;

    @Transactional
    public AttentionDto updateStatus(Long id, String newStatus, String notes) {
        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();

        Attention attention = attentionRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + id));

        String oldStatus = attention.getStatus();
        attention.setStatus(newStatus);

        if (notes != null && !notes.isBlank()) {
            if (attention.getNotes() != null && !attention.getNotes().isBlank()) {
                attention.setNotes(attention.getNotes() + "\n" + notes);
            } else {
                attention.setNotes(notes);
            }
        }

        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        if (Attention.STATUS_EN_PROCESO.equals(newStatus)) {
            if (attention.getStartTime() == null) {
                attention.setStartTime(now);
            }
        } else if (Attention.STATUS_ATENDIDA.equals(newStatus)) {
            if (attention.getEndTime() == null) {
                attention.setEndTime(now);
            }
            if (attention.getStartTime() != null && attention.getDurationMinutes() == null) {
                int minutes = (int) ChronoUnit.MINUTES.between(attention.getStartTime(), attention.getEndTime());
                attention.setDurationMinutes(Math.max(1, minutes));
            }
            if (attention.getAppointment() != null) {
                attention.getAppointment().setStatus("COMPLETADA");
                appointmentRepository.save(attention.getAppointment());
            }
        } else if (Attention.STATUS_COBRADA.equals(newStatus)) {
            if (attention.getEndTime() == null) {
                attention.setEndTime(now);
            }
            if (attention.getStartTime() != null && attention.getDurationMinutes() == null) {
                int minutes = (int) ChronoUnit.MINUTES.between(attention.getStartTime(), attention.getEndTime());
                attention.setDurationMinutes(Math.max(1, minutes));
            }
            if (attention.getAppointment() != null) {
                attention.getAppointment().setStatus("COMPLETADA");
                appointmentRepository.save(attention.getAppointment());
            }
        } else if (Attention.STATUS_CANCELADA.equals(newStatus)) {
            if (attention.getAppointment() != null && !"COMPLETADA".equals(attention.getAppointment().getStatus())) {
                attention.getAppointment().setStatus("CANCELADA");
                appointmentRepository.save(attention.getAppointment());
            }
        }

        Attention updated = attentionRepository.save(attention);

        auditLogService.record(
                "UPDATE",
                "ATTENTION",
                updated.getId().toString(),
                "Estado de atención cambiado de " + oldStatus + " a " + newStatus + " (ID: " + updated.getId() + ")"
        );

        return attentionMapper.toDto(updated);
    }
}
