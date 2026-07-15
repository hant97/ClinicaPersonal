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
          'CANCELLED': 'Cancelada'
        };

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
          upcomingAppointments
        };
      })
    );
  }
}
