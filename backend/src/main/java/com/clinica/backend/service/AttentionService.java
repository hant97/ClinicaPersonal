package com.clinica.backend.service;

import com.clinica.backend.dto.AttentionDto;
import com.clinica.backend.dto.AttentionSummaryDto;
import com.clinica.backend.dto.ProfessionalProductivityDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.mapper.AttentionMapper;
import com.clinica.backend.model.*;
import com.clinica.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttentionService {

    private final AttentionRepository attentionRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicalSessionRepository clinicalSessionRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PaymentRepository paymentRepository;
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

    @Transactional(readOnly = true)
    public AttentionSummaryDto getTodaySummary() {
        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();
        LocalDate today = LocalDate.now();

        List<Attention> todayAttentions = attentionRepository.findByAttentionDateAndSpecialtyAndDeletedFalse(today, specialty);

        long total = todayAttentions.size();
        long scheduled = 0;
        long inProgress = 0;
        long attended = 0;
        long paid = 0;
        long cancelled = 0;
        BigDecimal pendingBilling = BigDecimal.ZERO;

        for (Attention a : todayAttentions) {
            switch (a.getStatus()) {
                case Attention.STATUS_AGENDADA -> scheduled++;
                case Attention.STATUS_EN_PROCESO -> inProgress++;
                case Attention.STATUS_ATENDIDA -> {
                    attended++;
                    if (a.getPayment() == null || !Payment.STATUS_PAGADO.equals(a.getPayment().getStatus())) {
                        if (a.getClinicalService() != null && a.getClinicalService().getPrice() != null) {
                            pendingBilling = pendingBilling.add(a.getClinicalService().getPrice());
                        }
                    }
                }
                case Attention.STATUS_COBRADA -> paid++;
                case Attention.STATUS_CANCELADA -> cancelled++;
            }
        }

        return new AttentionSummaryDto(total, scheduled, inProgress, attended, paid, cancelled, pendingBilling);
    }

    @Transactional
    public AttentionDto create(AttentionDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();

        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + dto.getPatientId()));

        User professional = user;
        if (dto.getProfessionalId() != null && !dto.getProfessionalId().equals(user.getId())) {
            professional = userRepository.findById(dto.getProfessionalId())
                    .orElse(user);
        }

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

        User professional = user;
        if (appointment.getProfessionalId() != null) {
            professional = userRepository.findById(appointment.getProfessionalId()).orElse(user);
        }

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

    @Transactional
    public AttentionDto linkClinicalSession(Long attentionId, Long sessionId) {
        User user = clinicalAuthorizationService.currentProfessional();
        String specialty = user.getSpecialty();

        Attention attention = attentionRepository.findByIdAndSpecialtyAndDeletedFalse(attentionId, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + attentionId));

        ClinicalSession session = clinicalSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión clínica no encontrada con ID: " + sessionId));

        attention.setClinicalSession(session);
        session.setAttentionId(attention.getId());
        clinicalSessionRepository.save(session);
        Attention updated = attentionRepository.save(attention);

        auditLogService.record(
                "UPDATE",
                "ATTENTION",
                updated.getId().toString(),
                "Sesión clínica ID: " + sessionId + " vinculada a Atención ID: " + attentionId
        );

        return attentionMapper.toDto(updated);
    }

    @Transactional
    public AttentionDto linkPrescription(Long attentionId, Long prescriptionId) {
        User user = clinicalAuthorizationService.currentProfessional();
        String specialty = user.getSpecialty();

        Attention attention = attentionRepository.findByIdAndSpecialtyAndDeletedFalse(attentionId, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + attentionId));

        Prescription prescription = prescriptionRepository.findByIdAndDeletedFalse(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada con ID: " + prescriptionId));

        attention.setPrescription(prescription);
        prescription.setAttentionId(attention.getId());
        prescriptionRepository.save(prescription);
        Attention updated = attentionRepository.save(attention);

        auditLogService.record(
                "UPDATE",
                "ATTENTION",
                updated.getId().toString(),
                "Receta ID: " + prescriptionId + " vinculada a Atención ID: " + attentionId
        );

        return attentionMapper.toDto(updated);
    }

    @Transactional
    public AttentionDto linkPayment(Long attentionId, Long paymentId) {
        User user = clinicalAuthorizationService.currentUser();
        String specialty = user.getSpecialty();

        Attention attention = attentionRepository.findByIdAndSpecialtyAndDeletedFalse(attentionId, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Atención no encontrada con ID: " + attentionId));

        Payment payment = paymentRepository.findByIdAndDeletedFalse(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + paymentId));

        attention.setPayment(payment);
        payment.setAttentionId(attention.getId());

        if (Payment.STATUS_PAGADO.equals(payment.getStatus())) {
            attention.setStatus(Attention.STATUS_COBRADA);
        }

        paymentRepository.save(payment);
        Attention updated = attentionRepository.save(attention);

        auditLogService.record(
                "UPDATE",
                "ATTENTION",
                updated.getId().toString(),
                "Pago ID: " + paymentId + " vinculado a Atención ID: " + attentionId
        );

        return attentionMapper.toDto(updated);
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

    /**
     * Reporte gerencial de productividad por profesional dentro de la especialidad del
     * administrador autenticado. Agrupa las atenciones del período por profesional,
     * calculando volumen, tasa de finalización y montos facturados/cobrados.
     * Solo debe exponerse a usuarios con rol ADMIN (restricción aplicada en el controlador).
     */
    @Transactional(readOnly = true)
    public List<ProfessionalProductivityDto> getProfessionalProductivity(LocalDate dateFrom, LocalDate dateTo) {
        User admin = clinicalAuthorizationService.currentUser();
        String specialty = admin.getSpecialty();

        LocalDate today = LocalDate.now();
        LocalDate from = dateFrom != null ? dateFrom : today.withDayOfMonth(1);
        LocalDate to = dateTo != null ? dateTo : today;
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }

        List<Attention> attentions = attentionRepository
                .findBySpecialtyAndAttentionDateBetweenAndDeletedFalse(specialty, from, to);

        Map<Long, List<Attention>> byProfessional = attentions.stream()
                .filter(a -> a.getProfessional() != null)
                .collect(Collectors.groupingBy(a -> a.getProfessional().getId()));

        List<ProfessionalProductivityDto> result = new java.util.ArrayList<>();
        for (Map.Entry<Long, List<Attention>> entry : byProfessional.entrySet()) {
            List<Attention> list = entry.getValue();
            User professional = list.get(0).getProfessional();

            long total = list.size();
            long attended = list.stream().filter(a -> Attention.STATUS_ATENDIDA.equals(a.getStatus()) || Attention.STATUS_COBRADA.equals(a.getStatus())).count();
            long cancelled = list.stream().filter(a -> Attention.STATUS_CANCELADA.equals(a.getStatus())).count();
            long paid = list.stream().filter(a -> Attention.STATUS_COBRADA.equals(a.getStatus())).count();

            BigDecimal billed = list.stream()
                    .filter(a -> Attention.STATUS_ATENDIDA.equals(a.getStatus()) || Attention.STATUS_COBRADA.equals(a.getStatus()))
                    .map(a -> a.getClinicalService() != null && a.getClinicalService().getPrice() != null
                            ? a.getClinicalService().getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal collected = list.stream()
                    .filter(a -> a.getPayment() != null && Payment.STATUS_PAGADO.equals(a.getPayment().getStatus()))
                    .map(a -> a.getPayment().getAmount() != null ? a.getPayment().getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long completionBase = total - cancelled;
            int completionRate = completionBase > 0 ? (int) Math.round((attended * 100.0) / completionBase) : 0;

            result.add(ProfessionalProductivityDto.builder()
                    .professionalId(entry.getKey())
                    .professionalName(professional.getFullName())
                    .totalAttentions(total)
                    .attendedAttentions(attended)
                    .cancelledAttentions(cancelled)
                    .paidAttentions(paid)
                    .completionRate(completionRate)
                    .billedAmount(billed)
                    .collectedAmount(collected)
                    .build());
        }

        result.sort((a, b) -> b.getBilledAmount().compareTo(a.getBilledAmount()));
        return result;
    }
}
