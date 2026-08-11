package com.clinica.backend.service;

import com.clinica.backend.dto.DashboardAppointmentDto;
import com.clinica.backend.dto.DashboardRiskAlertDto;
import com.clinica.backend.dto.DashboardStatsDto;
import com.clinica.backend.dto.SupplyDto;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.RiskAlert;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.RiskAlertRepository;
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

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        YearMonth currentYearMonth = YearMonth.from(today);
        YearMonth previousYearMonth = currentYearMonth.minusMonths(1);

        LocalDateTime startOfCurrentMonth = currentYearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfCurrentMonth = currentYearMonth.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime startOfPreviousMonth = previousYearMonth.atDay(1).atStartOfDay();

        // 1. Pacientes activos
        long activePatients = patientRepository.countByDeletedFalse();

        // 2. Citas hoy
        long appointmentsToday = appointmentRepository.countByAppointmentDate(today);

        // 3. Cobros del mes actual y mes anterior
        BigDecimal monthlyIncome = paymentRepository.sumIncomeBetween(startOfCurrentMonth, endOfCurrentMonth);
        BigDecimal previousMonthlyIncome = paymentRepository.sumIncomeBetween(startOfPreviousMonth, startOfCurrentMonth);

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
        List<Appointment> currentMonthAppointments = appointmentRepository.findByAppointmentDateBetween(
                currentYearMonth.atDay(1),
                currentYearMonth.atEndOfMonth()
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
        long newPatientsThisMonth = patientRepository.countNewPatientsBetween(startOfCurrentMonth, endOfCurrentMonth);

        // 6. Próximas citas (top 5)
        List<Appointment> upcomingList = appointmentRepository.findUpcomingAppointments(
                today,
                nowTime,
                PageRequest.of(0, 5)
        );

        List<DashboardAppointmentDto> upcomingAppointments = upcomingList.stream()
                .map(app -> {
                    String patientName = app.getPatient() != null
                            ? (app.getPatient().getFirstName() + " " + app.getPatient().getLastName()).trim()
                            : "Paciente #" + app.getId();
                    return DashboardAppointmentDto.builder()
                            .id(app.getId())
                            .patientId(app.getPatient() != null ? app.getPatient().getId() : null)
                            .patientName(patientName)
                            .appointmentDate(app.getAppointmentDate())
                            .startTime(app.getStartTime())
                            .endTime(app.getEndTime())
                            .status(app.getStatus())
                            .modality(app.getModality())
                            .videoCallLink(app.getVideoCallLink())
                            .isFirstTime(app.isFirstTime())
                            .notes(app.getNotes())
                            .build();
                })
                .collect(Collectors.toList());

        // 7. Alertas de riesgo activas
        Page<RiskAlert> riskAlertsPage = riskAlertRepository.findByActiveTrueOrderByCreatedAtDesc(PageRequest.of(0, 10));
        List<RiskAlert> riskAlerts = riskAlertsPage.getContent();

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

        return DashboardStatsDto.builder()
                .activePatients(activePatients)
                .appointmentsToday(appointmentsToday)
                .monthlyIncome(monthlyIncome != null ? monthlyIncome : BigDecimal.ZERO)
                .upcomingAppointments(upcomingAppointments)
                .attendanceRate(attendanceRate)
                .cancelledAppointments(cancelledAppointments)
                .newPatientsThisMonth(newPatientsThisMonth)
                .monthlyIncomeGrowth(monthlyIncomeGrowth)
                .activeRiskAlerts(activeRiskAlerts)
                .lowStockSupplies(lowStockSupplies)
                .build();
    }
}
