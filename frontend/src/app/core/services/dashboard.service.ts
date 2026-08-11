import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Appointment } from '../models/appointment.model';
import { RiskAlert } from '../models/risk-alert.model';
import { Supply } from './inventory.service';

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
  private apiUrl = `${environment.apiUrl}/v1/dashboard`;

  private readonly statusTranslations: { [key: string]: string } = {
    'PROGRAMADA': 'Programada',
    'COMPLETADA': 'Completada',
    'CANCELADA': 'Cancelada',
    'NO_ASISTIO': 'No Asistió',
    'CONFIRMADA': 'Confirmada'
  };

  constructor(private http: HttpClient) { }

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/stats`).pipe(
      map(stats => ({
        ...stats,
        upcomingAppointments: (stats.upcomingAppointments || []).map(app => ({
          ...app,
          status: this.statusTranslations[app.status] || app.status
        }))
      }))
    );
  }
}
