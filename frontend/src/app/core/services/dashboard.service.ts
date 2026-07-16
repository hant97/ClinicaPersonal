import { Injectable } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { PatientService } from './patient/patient.service';
import { AppointmentService } from './appointment.service';
import { PaymentService } from './payment.service';
import { InventoryService, Supply } from './inventory.service';
import { RiskAlertService } from './risk-alert.service';
import { Appointment } from '../models/appointment.model';
import { RiskAlert } from '../models/risk-alert.model';

export interface DashboardStats {
  activePatients: number;
  appointmentsToday: number;
  monthlyIncome: number;
  upcomingAppointments: Appointment[];
  attendanceRate: number;
  cancelledAppointments: number;
  newPatientsThisMonth: number;
  monthlyIncomeGrowth: number;
  activeRiskAlerts: RiskAlert[];
  lowStockSupplies: Supply[];
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  constructor(
    private patientService: PatientService,
    private appointmentService: AppointmentService,
    private paymentService: PaymentService,
    private inventoryService: InventoryService,
    private riskAlertService: RiskAlertService
  ) { }

  getDashboardStats(): Observable<DashboardStats> {
    return forkJoin({
      patients: this.patientService.getAll(0, 1000), // Usamos 1000 para cargar la mayoría de pacientes para las estadísticas del dashboard
      appointments: this.appointmentService.getAll(),
      payments: this.paymentService.getAll(0, 1000),
      lowStockSupplies: this.inventoryService.getLowStockSupplies(),
      activeRiskAlerts: this.riskAlertService.getAllActiveAlerts()
    }).pipe(
      map(({ patients, appointments, payments, lowStockSupplies, activeRiskAlerts }) => {
        const today = new Date();
        const startOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
        const endOfDay = startOfDay + 24 * 60 * 60 * 1000;
        const currentMonth = today.getMonth();
        const currentYear = today.getFullYear();

        const activePatients = patients.page.totalElements;
        const patientsList = patients.content;

        const appointmentsToday = appointments.filter(app => {
          const appDate = new Date(app.appointmentDate + 'T00:00:00').getTime();
          return appDate >= startOfDay && appDate < endOfDay;
        }).length;

        const monthlyIncome = payments.content.filter(pay => {
          const payDate = new Date(pay.paymentDate);
          return payDate.getMonth() === currentMonth && payDate.getFullYear() === currentYear;
        }).reduce((sum, pay) => sum + pay.amount, 0);

        const statusTranslations: { [key: string]: string } = {
          'SCHEDULED': 'Programada',
          'COMPLETED': 'Completada',
          'CANCELLED': 'Cancelada',
          'NO_ASISTIO': 'No Asistió',
          'CONFIRMADA': 'Confirmada'
        };

        // Nuevos cálculos
        // 1. Tasa de Asistencia y Cancelaciones (Mes Actual)
        const appointmentsThisMonth = appointments.filter(app => {
          const appDate = new Date(app.appointmentDate + 'T00:00:00');
          return appDate.getMonth() === currentMonth && appDate.getFullYear() === currentYear;
        });
        
        const completedThisMonth = appointmentsThisMonth.filter(app => app.status === 'COMPLETED').length;
        const cancelledAppointments = appointmentsThisMonth.filter(app => app.status === 'CANCELLED' || app.status === 'NO_ASISTIO').length;
        const totalPastThisMonth = completedThisMonth + cancelledAppointments;
        const attendanceRate = totalPastThisMonth > 0 ? Math.round((completedThisMonth / totalPastThisMonth) * 100) : 0;

        // 2. Nuevos Pacientes
        const newPatientsThisMonth = patientsList.filter(p => {
          if (!p.createdAt) return false;
          const pDate = new Date(p.createdAt);
          return pDate.getMonth() === currentMonth && pDate.getFullYear() === currentYear;
        }).length;

        // 3. Crecimiento de Ingresos
        const previousMonth = currentMonth === 0 ? 11 : currentMonth - 1;
        const previousMonthYear = currentMonth === 0 ? currentYear - 1 : currentYear;
        
        const previousMonthlyIncome = payments.content.filter(pay => {
          const payDate = new Date(pay.paymentDate);
          return payDate.getMonth() === previousMonth && payDate.getFullYear() === previousMonthYear;
        }).reduce((sum, pay) => sum + pay.amount, 0);

        let monthlyIncomeGrowth = 0;
        if (previousMonthlyIncome > 0) {
          monthlyIncomeGrowth = Math.round(((monthlyIncome - previousMonthlyIncome) / previousMonthlyIncome) * 100);
        } else if (monthlyIncome > 0) {
          monthlyIncomeGrowth = 100; // Crecimiento del 100% si el mes pasado fue 0 y este mes hay ingresos
        }

        const upcomingAppointments = appointments
          .filter(app => {
            const appDateTimeStr = app.startTime ? `${app.appointmentDate}T${app.startTime}` : `${app.appointmentDate}T00:00:00`;
            const appDateTime = new Date(appDateTimeStr);
            return appDateTime.getTime() >= today.getTime() && 
                   app.status !== 'CANCELLED' && 
                   app.status !== 'NO_ASISTIO' && 
                   app.status !== 'COMPLETED';
          })
          .sort((a, b) => {
            const dateA = new Date(a.startTime ? `${a.appointmentDate}T${a.startTime}` : `${a.appointmentDate}T00:00:00`).getTime();
            const dateB = new Date(b.startTime ? `${b.appointmentDate}T${b.startTime}` : `${b.appointmentDate}T00:00:00`).getTime();
            return dateA - dateB;
          })
          .slice(0, 5) // top 5 upcoming
          .map(app => {
            const patient = patientsList.find(p => p.id === app.patientId);
            return {
              ...app,
              patientName: patient ? `${patient.firstName} ${patient.lastName}` : `Paciente ID: ${app.patientId}`,
              status: statusTranslations[app.status] || app.status
            };
          });

        return {
          activePatients,
          appointmentsToday,
          monthlyIncome,
          upcomingAppointments,
          attendanceRate,
          cancelledAppointments,
          newPatientsThisMonth,
          monthlyIncomeGrowth,
          activeRiskAlerts,
          lowStockSupplies
        };
      })
    );
  }
}
