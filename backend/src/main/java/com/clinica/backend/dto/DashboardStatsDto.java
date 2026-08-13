package com.clinica.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long activePatients;
    private long appointmentsToday;
    private BigDecimal monthlyIncome;
    private List<DashboardAppointmentDto> upcomingAppointments;
    private int attendanceRate;
    private long cancelledAppointments;
    private long newPatientsThisMonth;
    private int monthlyIncomeGrowth;
    private List<DashboardRiskAlertDto> activeRiskAlerts;
    private List<SupplyDto> lowStockSupplies;

    // Specialty specific metrics
    private long psychometricEvaluationsThisMonth;
    private long dermatologicalEvaluationsThisMonth;
    private long dermatologicalProceduresThisMonth;
}
