package com.clinica.backend.service;

import com.clinica.backend.dto.DashboardAppointmentDto;
import com.clinica.backend.dto.DashboardPendingNoteDto;
import com.clinica.backend.dto.DashboardRiskAlertDto;
import com.clinica.backend.dto.DashboardStatsDto;
import com.clinica.backend.dto.SupplyDto;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Payment;
import com.clinica.backend.model.RiskAlert;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.RiskAlertRepository;
import com.clinica.backend.repository.AssessmentRepository;
import com.clinica.backend.repository.DermatologicalEvaluationRepository;
import com.clinica.backend.repository.CatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final SupplyService supplyService;
    private final AssessmentRepository assessmentRepository;
    private final DermatologicalEvaluationRepository dermatologicalEvaluationRepository;
    private final CatalogRepository catalogRepository;
    private final ClinicalSessionRepository clinicalSessionRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(String specialty) {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        YearMonth currentYearMonth = YearMonth.from(today);
        YearMonth previousYearMonth = currentYearMonth.minusMonths(1);

        LocalDateTime startOfCurrentMonth = currentYearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfCurrentMonth = currentYearMonth.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime startOfPreviousMonth = previousYearMonth.atDay(1).atStartOfDay();

        // 1. Pacientes activos
        long activePatients = patientRepository.countBySpecialtyAndDeletedFalse(specialty);

        // 2. Citas hoy
        long appointmentsToday = appointmentRepository.countByAppointmentDateAndSpecialty(today, specialty);

        // 3. Cobros del mes actual y mes anterior
        BigDecimal monthlyIncome = paymentRepository.sumIncomeBetweenBySpecialty(startOfCurrentMonth, endOfCurrentMonth, specialty);
        BigDecimal previousMonthlyIncome = paymentRepository.sumIncomeBetweenBySpecialty(startOfPreviousMonth, startOfCurrentMonth, specialty);

        int monthlyIncomeGrowth = 0;
        if (previousMonthlyIncome != null && previousMonthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = monthlyIncome.subtract(previousMonthlyIncome);
            monthlyIncomeGrowth = diff.multiply(BigDecimal.valueOf(100))
                    .divide(previousMonthlyIncome, 0, RoundingMode.HALF_UP)
                    .intValue();
        } else if (monthlyIncome != null && monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            monthlyIncomeGrowth = 100;
        }

        // 4. Asistencia y cancelaciones del mes actual
        List<Appointment> currentMonthAppointments = appointmentRepository.findByAppointmentDateBetweenAndSpecialty(
                currentYearMonth.atDay(1),
                currentYearMonth.atEndOfMonth(),
                specialty
        );

        long completedThisMonth = currentMonthAppointments.stream()
                .filter(a -> "COMPLETADA".equalsIgnoreCase(a.getStatus()))
                .count();

        long cancelledAppointments = currentMonthAppointments.stream()
                .filter(a -> "CANCELADA".equalsIgnoreCase(a.getStatus()) || "NO_ASISTIO".equalsIgnoreCase(a.getStatus()))
                .count();

        long totalPast = completedThisMonth + cancelledAppointments;
        int attendanceRate = totalPast > 0 ? (int) Math.round((completedThisMonth * 100.0) / totalPast) : 0;

        // 5. Nuevos pacientes este mes
        long newPatientsThisMonth = patientRepository.countNewPatientsBetween(specialty, startOfCurrentMonth, endOfCurrentMonth);

        // 6. Próximas citas (top 5)
        List<Appointment> upcomingList = appointmentRepository.findUpcomingAppointmentsBySpecialty(
                today,
                nowTime,
                specialty,
                PageRequest.of(0, 5)
        );

        // 6b. Citas del día (agenda completa, con estado real)
        List<Appointment> todaysList = appointmentRepository
                .findTodayAppointmentsBySpecialty(today, specialty);

        List<Long> dashboardAppIds = new ArrayList<>();
        upcomingList.forEach(a -> { if (a.getId() != null) dashboardAppIds.add(a.getId()); });
        todaysList.forEach(a -> { if (a.getId() != null) dashboardAppIds.add(a.getId()); });

        Map<Long, Payment> dashboardPayments = new HashMap<>();
        if (!dashboardAppIds.isEmpty()) {
            paymentRepository.findByAppointmentIdInAndDeletedFalse(dashboardAppIds)
                    .forEach(p -> {
                        if (p.getAppointment() != null && p.getAppointment().getId() != null) {
                            dashboardPayments.putIfAbsent(p.getAppointment().getId(), p);
                        }
                    });
        }

        List<DashboardAppointmentDto> upcomingAppointments = upcomingList.stream()
                .map(a -> toDashboardAppointmentDto(a, dashboardPayments.get(a.getId())))
                .collect(Collectors.toList());

        List<DashboardAppointmentDto> todaysAppointments = todaysList.stream()
                .map(a -> toDashboardAppointmentDto(a, dashboardPayments.get(a.getId())))
                .collect(Collectors.toList());

        // 7. Alertas de riesgo activas (filtradas por especialidad si es necesario)
        // Para simplificar, asumimos que obtenemos todas las activas, pero deberíamos
        // filtrar por las que correspondan al catálogo de la especialidad
        Page<RiskAlert> riskAlertsPage = riskAlertRepository.findBySpecialtyAndActiveTrueOrderByCreatedAtDesc(specialty, PageRequest.of(0, 50));
        List<RiskAlert> riskAlerts = riskAlertsPage.getContent();

        // Obtenemos los items del catálogo de riesgos de la especialidad actual
        Set<String> validAlertTypes = new HashSet<>();
        List<String> candidateCodes = List.of("RISK_ALERT_TYPE_" + (specialty != null ? specialty.toUpperCase() : ""), "RISK_ALERT_TYPE", "RISK_ALERT_TYPE_DERM");
        for (String code : candidateCodes) {
            catalogRepository.findByCode(code).ifPresent(catalog -> {
                if (specialty == null || "ALL".equalsIgnoreCase(specialty) || "GENERAL".equalsIgnoreCase(catalog.getSpecialty()) || specialty.equalsIgnoreCase(catalog.getSpecialty())) {
                    catalog.getItems().stream()
                            .filter(com.clinica.backend.model.CatalogItem::isActive)
                            .forEach(item -> validAlertTypes.add(item.getItemName()));
                }
            });
            if (!validAlertTypes.isEmpty()) {
                break;
            }
        }

        // Filtramos las alertas
        if (!validAlertTypes.isEmpty()) {
            riskAlerts = riskAlerts.stream()
                    .filter(alert -> validAlertTypes.contains(alert.getType()))
                    .limit(10)
                    .collect(Collectors.toList());
        } else {
            riskAlerts = riskAlerts.stream().limit(10).collect(Collectors.toList());
        }

        Set<Long> patientIds = riskAlerts.stream()
                .map(RiskAlert::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> patientNamesMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            List<Patient> patients = patientRepository.findAllById(patientIds);
            for (Patient p : patients) {
                patientNamesMap.put(p.getId(), (p.getFirstName() + " " + p.getLastName()).trim());
            }
        }

        List<DashboardRiskAlertDto> activeRiskAlerts = riskAlerts.stream()
                .map(alert -> DashboardRiskAlertDto.builder()
                        .id(alert.getId())
                        .patientId(alert.getPatientId())
                        .patientName(patientNamesMap.getOrDefault(alert.getPatientId(), "Paciente #" + alert.getPatientId()))
                        .type(alert.getType())
                        .level(alert.getLevel())
                        .description(alert.getDescription())
                        .active(alert.isActive())
                        .resolvedAt(alert.getResolvedAt())
                        .createdAt(alert.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // 8. Insumos con bajo stock
        List<SupplyDto> lowStockSupplies = supplyService.getLowStockSupplies();

        // 8b. Notas SOAP pendientes de registro
        List<DashboardPendingNoteDto> pendingSoapNotes = clinicalSessionRepository
                .findPendingNotesBySpecialty(specialty, today, PageRequest.of(0, 10)).stream()
                .map(s -> DashboardPendingNoteDto.builder()
                        .id(s.getId())
                        .patientId(s.getPatient() != null ? s.getPatient().getId() : null)
                        .patientName(s.getPatient() != null
                                ? (s.getPatient().getFirstName() + " " + s.getPatient().getLastName()).trim()
                                : "Paciente #" + s.getId())
                        .sessionDate(s.getSessionDate())
                        .sessionType(s.getSessionType())
                        .build())
                .collect(Collectors.toList());

        // 9. Métricas por especialidad
        long psychometricEvaluationsThisMonth = 0;
        long dermatologicalEvaluationsThisMonth = 0;
        long dermatologicalProceduresThisMonth = 0;

        if ("PSICOLOGIA".equals(specialty)) {
            psychometricEvaluationsThisMonth = assessmentRepository.countByAssessmentDateBetween(startOfCurrentMonth, endOfCurrentMonth);
        } else if ("DERMATOLOGIA".equals(specialty)) {
            LocalDate startDate = currentYearMonth.atDay(1);
            LocalDate endDate = currentYearMonth.plusMonths(1).atDay(1);
            dermatologicalEvaluationsThisMonth = dermatologicalEvaluationRepository.countByEvaluationDateBetween(startDate, endDate);
            dermatologicalProceduresThisMonth = dermatologicalEvaluationRepository.countByEvaluationDateBetweenAndProcedurePerformedIsNotNullAndProcedurePerformedNot(startDate, endDate, "");
        }

        return DashboardStatsDto.builder()
                .activePatients(activePatients)
                .appointmentsToday(appointmentsToday)
                .monthlyIncome(monthlyIncome != null ? monthlyIncome : BigDecimal.ZERO)
                .upcomingAppointments(upcomingAppointments)
                .todaysAppointments(todaysAppointments)
                .attendanceRate(attendanceRate)
                .cancelledAppointments(cancelledAppointments)
                .newPatientsThisMonth(newPatientsThisMonth)
                .monthlyIncomeGrowth(monthlyIncomeGrowth)
                .activeRiskAlerts(activeRiskAlerts)
                .lowStockSupplies(lowStockSupplies)
                .pendingSoapNotes(pendingSoapNotes)
                .psychometricEvaluationsThisMonth(psychometricEvaluationsThisMonth)
                .dermatologicalEvaluationsThisMonth(dermatologicalEvaluationsThisMonth)
                .dermatologicalProceduresThisMonth(dermatologicalProceduresThisMonth)
                .build();
    }

    private DashboardAppointmentDto toDashboardAppointmentDto(Appointment app, Payment payment) {
        String patientName = app.getPatient() != null
                ? (app.getPatient().getFirstName() + " " + app.getPatient().getLastName()).trim()
                : "Paciente #" + app.getId();
        return DashboardAppointmentDto.builder()
                .id(app.getId())
                .patientId(app.getPatient() != null ? app.getPatient().getId() : null)
                .patientUuid(app.getPatient() != null && app.getPatient().getUuid() != null ? app.getPatient().getUuid().toString() : null)
                .patientName(patientName)
                .appointmentDate(app.getAppointmentDate())
                .startTime(app.getStartTime())
                .endTime(app.getEndTime())
                .status(app.getStatus())
                .modality(app.getModality())
                .videoCallLink(app.getVideoCallLink())
                .isFirstTime(app.isFirstTime())
                .notes(app.getNotes())
                .isPaid(payment != null)
                .paymentId(payment != null ? payment.getId() : null)
                .paymentAmount(payment != null ? payment.getAmount() : null)
                .build();
    }
}
