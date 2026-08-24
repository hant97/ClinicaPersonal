package com.clinica.backend.service;

import com.clinica.backend.dto.PatientBalanceDto;
import com.clinica.backend.dto.PaymentSummaryDto;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentReportService {
    private static final int MAX_REPORT_RANGE_DAYS = 366;

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public PaymentSummaryDto getSummary(LocalDate dateFrom, LocalDate dateTo) {
        String specialty = getCurrentUserSpecialty();
        LocalDate today = LocalDate.now();
        BigDecimal incomeToday = paymentTransactionRepository.sumIncomeBetweenBySpecialty(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay(), specialty);

        LocalDateTime rangeStart;
        LocalDateTime rangeEnd;
        LocalDate chartStart;
        long rangeDays;
        boolean customRange = dateFrom != null || dateTo != null;
        if (customRange) {
            LocalDate from = dateFrom != null ? dateFrom : LocalDate.of(1970, 1, 1);
            LocalDate to = dateTo != null ? dateTo : today;
            if (to.isBefore(from)) {
                throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
            }
            rangeDays = ChronoUnit.DAYS.between(from, to) + 1;
            if (rangeDays > MAX_REPORT_RANGE_DAYS) {
                throw new IllegalArgumentException("El rango máximo del reporte es de " + MAX_REPORT_RANGE_DAYS + " días");
            }
            rangeStart = from.atStartOfDay();
            rangeEnd = to.plusDays(1).atStartOfDay();
            chartStart = from;
        } else {
            YearMonth currentMonth = YearMonth.from(today);
            rangeStart = currentMonth.atDay(1).atStartOfDay();
            rangeEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
            chartStart = today.minusDays(29);
            rangeDays = 30;
        }

        BigDecimal incomePeriod = paymentTransactionRepository.sumIncomeBetweenBySpecialty(rangeStart, rangeEnd, specialty);
        long paymentsCount = paymentTransactionRepository.countBetweenBySpecialty(rangeStart, rangeEnd, specialty);
        LocalDateTime previousEnd = rangeStart;
        LocalDateTime previousStart = customRange
                ? rangeStart.minusDays(rangeDays)
                : YearMonth.from(today).minusMonths(1).atDay(1).atStartOfDay();
        BigDecimal incomePrevious = paymentTransactionRepository.sumIncomeBetweenBySpecialty(previousStart, previousEnd, specialty);

        int monthlyGrowth = 0;
        if (incomePrevious != null && incomePrevious.compareTo(BigDecimal.ZERO) > 0) {
            monthlyGrowth = incomePeriod.subtract(incomePrevious)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(incomePrevious, 0, RoundingMode.HALF_UP)
                    .intValue();
        } else if (incomePeriod != null && incomePeriod.compareTo(BigDecimal.ZERO) > 0) {
            monthlyGrowth = 100;
        }

        BigDecimal safeIncomePeriod = incomePeriod != null ? incomePeriod : BigDecimal.ZERO;
        BigDecimal averageTicket = paymentsCount > 0
                ? safeIncomePeriod.divide(BigDecimal.valueOf(paymentsCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        List<PaymentSummaryDto.MethodSummary> methodBreakdown = paymentTransactionRepository
                .sumByMethodBetweenBySpecialty(rangeStart, rangeEnd, specialty).stream()
                .map(row -> PaymentSummaryDto.MethodSummary.builder()
                        .method(row[0] != null ? row[0].toString() : "OTRO")
                        .total((BigDecimal) row[1])
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        LocalDateTime chartEnd = customRange ? rangeEnd : today.plusDays(1).atStartOfDay();
        Map<LocalDate, BigDecimal> incomeByDay = new HashMap<>();
        for (Object[] row : paymentTransactionRepository.sumDailyIncomeBetweenBySpecialty(
                chartStart.atStartOfDay(), chartEnd, specialty)) {
            incomeByDay.put(toLocalDate(row[0]), (BigDecimal) row[1]);
        }
        long chartDays = customRange ? rangeDays : 30;
        List<PaymentSummaryDto.DailyIncome> dailyIncome = new ArrayList<>();
        for (int i = 0; i < chartDays; i++) {
            LocalDate day = chartStart.plusDays(i);
            dailyIncome.add(PaymentSummaryDto.DailyIncome.builder()
                    .date(day)
                    .total(incomeByDay.getOrDefault(day, BigDecimal.ZERO))
                    .build());
        }

        List<PaymentSummaryDto.ServiceSummary> topServices = paymentRepository
                .findTopServicesBySpecialty(rangeStart, rangeEnd, specialty, PageRequest.of(0, 5)).stream()
                .map(row -> PaymentSummaryDto.ServiceSummary.builder()
                        .name(row[0].toString())
                        .quantity(((Number) row[1]).longValue())
                        .total((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());
        BigDecimal totalCharged = paymentRepository.sumChargedBySpecialty(specialty);
        BigDecimal totalReceived = paymentTransactionRepository.sumReceivedBySpecialty(specialty);

        return PaymentSummaryDto.builder()
                .incomeToday(incomeToday != null ? incomeToday : BigDecimal.ZERO)
                .incomeMonth(safeIncomePeriod)
                .monthlyGrowth(monthlyGrowth)
                .paymentsCountMonth(paymentsCount)
                .averageTicket(averageTicket)
                .pendingBalance(totalCharged.subtract(totalReceived).max(BigDecimal.ZERO))
                .methodBreakdown(methodBreakdown)
                .dailyIncome(dailyIncome)
                .topServices(topServices)
                .build();
    }

    @Transactional(readOnly = true)
    public PatientBalanceDto getPatientBalance(Long patientId) {
        String specialty = getCurrentUserSpecialty();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, specialty)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
        BigDecimal totalCharged = paymentRepository.sumChargedByPatientAndSpecialty(patient.getId(), specialty);
        BigDecimal totalPaid = paymentTransactionRepository.sumReceivedByPatientAndSpecialty(patient.getId(), specialty);

        return PatientBalanceDto.builder()
                .patientId(patient.getId())
                .totalCharged(totalCharged)
                .totalPaid(totalPaid)
                .balance(totalCharged.subtract(totalPaid).max(BigDecimal.ZERO))
                .build();
    }

    private String getCurrentUserSpecialty() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSpecialty();
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }
}
