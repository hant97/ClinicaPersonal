import { Injectable } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { PatientService } from './patient/patient.service';
import { AppointmentService } from './appointment.service';
import { PaymentService } from './payment.service';
import { Appointment } from '../models/appointment.model';

export interface DashboardStats {
  activePatients: number;
  appointmentsToday: number;
  monthlyIncome: number;
  upcomingAppointments: Appointment[];
  attendanceRate: number;
  cancelledAppointments: number;
  newPatientsThisMonth: number;
  monthlyIncomeGrowth: number;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  constructor(
    private patientService: PatientService,
    private appointmentService: AppointmentService,
    private paymentService: PaymentService
  ) { }

  getDashboardStats(): Observable<DashboardStats> {
    return forkJoin({
      patients: this.patientService.getAll(),
      appointments: this.appointmentService.getAll(),
      payments: this.paymentService.getAll()
    }).pipe(
      map(({ patients, appointments, payments }) => {
        const today = new Date();
        const startOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
        const endOfDay = startOfDay + 24 * 60 * 60 * 1000;
        const currentMonth = today.getMonth();
        const currentYear = today.getFullYear();

        const activePatients = patients.length;

        const appointmentsToday = appointments.filter(app => {
          const appDate = new Date(app.appointmentDate).getTime();
          return appDate >= startOfDay && appDate < endOfDay;
        }).length;

        const monthlyIncome = payments.filter(pay => {
          const payDate = new Date(pay.paymentDate);
          return payDate.getMonth() === currentMonth && payDate.getFullYear() === currentYear;
        }).reduce((sum, pay) => sum + pay.amount, 0);

        const statusTranslations: { [key: string]: string } = {
          'SCHEDULED': 'Programada',
          'COMPLETED': 'Completada',
          'CANCELLED': 'Cancelada',
          'NO_ASISTIO': 'No Asistió'
        };

        // Nuevos cálculos
        // 1. Tasa de Asistencia y Cancelaciones (Mes Actual)
        const appointmentsThisMonth = appointments.filter(app => {
          const appDate = new Date(app.appointmentDate);
          return appDate.getMonth() === currentMonth && appDate.getFullYear() === currentYear;
        });
        
        const completedThisMonth = appointmentsThisMonth.filter(app => app.status === 'COMPLETED').length;
        const cancelledAppointments = appointmentsThisMonth.filter(app => app.status === 'CANCELLED' || app.status === 'NO_ASISTIO').length;
        const totalPastThisMonth = completedThisMonth + cancelledAppointments;
        const attendanceRate = totalPastThisMonth > 0 ? Math.round((completedThisMonth / totalPastThisMonth) * 100) : 0;

        // 2. Nuevos Pacientes
        const newPatientsThisMonth = patients.filter(p => {
          if (!p.createdAt) return false;
          const pDate = new Date(p.createdAt);
          return pDate.getMonth() === currentMonth && pDate.getFullYear() === currentYear;
        }).length;

        // 3. Crecimiento de Ingresos
        const previousMonth = currentMonth === 0 ? 11 : currentMonth - 1;
        const previousMonthYear = currentMonth === 0 ? currentYear - 1 : currentYear;
        
        const previousMonthlyIncome = payments.filter(pay => {
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
          .filter(app => new Date(app.appointmentDate).getTime() >= today.getTime())
          .sort((a, b) => new Date(a.appointmentDate).getTime() - new Date(b.appointmentDate).getTime())
          .slice(0, 5) // top 5 upcoming
          .map(app => {
            const patient = patients.find(p => p.id === app.patientId);
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
          monthlyIncomeGrowth
        };
      })
    );
  }
}
