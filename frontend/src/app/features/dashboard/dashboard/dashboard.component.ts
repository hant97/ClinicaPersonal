import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { DashboardService, DashboardStats } from '../../../core/services/dashboard.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { Appointment } from '../../../core/models/appointment.model';
import { StatusPillComponent, StatusPillVariant } from '../../../shared/components/status-pill/status-pill.component';
import {
  LucideAngularModule,
  Users,
  Calendar,
  DollarSign,
  TrendingUp,
  TrendingDown,
  Clock,
  AlertTriangle,
  CheckCircle2,
  ArrowRight,
  Plus,
  UserPlus,
  Zap,
  Play
} from 'lucide-angular';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule, StatusPillComponent],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  isLoading = true;
  loadError = false;

  isPsychology = false;
  isDermatology = false;
  dashboardTitle = 'Panel Operativo';

  // Next Patient in Queue
  nextAppointment: Appointment | null = null;

  // Agenda del día
  todaysAppointments: Appointment[] = [];

  // Pestaña activa de la sección "Atención Requerida"
  activeAttentionTab: 'alertas' | 'insumos' | 'notas' = 'alertas';

  readonly Users = Users;
  readonly Calendar = Calendar;
  readonly DollarSign = DollarSign;
  readonly TrendingUp = TrendingUp;
  readonly TrendingDown = TrendingDown;
  readonly Clock = Clock;
  readonly AlertTriangle = AlertTriangle;
  readonly CheckCircle2 = CheckCircle2;
  readonly ArrowRight = ArrowRight;
  readonly Plus = Plus;
  readonly UserPlus = UserPlus;
  readonly Zap = Zap;
  readonly Play = Play;

  constructor(
    private dashboardService: DashboardService,
    private specialtyService: SpecialtyService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isPsychology = this.specialtyService.isPsychology();
    this.isDermatology = this.specialtyService.isDermatology();
    this.dashboardTitle = this.isPsychology
      ? 'Panel Operativo — Psicología'
      : this.isDermatology
      ? 'Panel Operativo — Dermatología'
      : 'Panel Operativo FlowGrid';
    this.loadStats();
  }

  loadStats(): void {
    this.isLoading = true;
    this.loadError = false;
    this.dashboardService.getDashboardStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.todaysAppointments = stats.todaysAppointments || [];
        this.resolveNextAppointment(stats.upcomingAppointments || []);
        this.isLoading = false;
      },
      error: () => {
        this.stats = null;
        this.isLoading = false;
        this.loadError = true;
      }
    });
  }

  private resolveNextAppointment(appointments: Appointment[]): void {
    const active = appointments.filter(
      (a) => a.status !== 'Cancelada' && a.status !== 'CANCELADA' && a.status !== 'Completada' && a.status !== 'COMPLETADA'
    );
    this.nextAppointment = active.length > 0 ? active[0] : (appointments.length > 0 ? appointments[0] : null);
  }

  get pendingAppointmentsCount(): number {
    return this.todaysAppointments.filter(
      (a) => a.status === 'Programada' || a.status === 'Confirmada'
    ).length;
  }

  get attentionTotal(): number {
    if (!this.stats) return 0;
    return this.stats.activeRiskAlerts.length
      + this.stats.lowStockSupplies.length
      + this.stats.pendingSoapNotes.length;
  }

  get nextAppointmentLabel(): string {
    const app = this.nextAppointment;
    if (!app) return 'sin citas pendientes';
    const time = app.startTime ? app.startTime.substring(0, 5) : 'hoy';
    if (app.appointmentDate && app.appointmentDate !== this.todayKey()) {
      const [y, m, d] = app.appointmentDate.split('-');
      const fecha = d && m && y ? `${d}/${m}/${y}` : app.appointmentDate;
      return `próxima el ${fecha} a las ${time}`;
    }
    return `próxima a las ${time}`;
  }

  private todayKey(): string {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
  }

  getAppointmentStatusVariant(status: string): StatusPillVariant {
    switch (status) {
      case 'Completada':
        return 'stable';
      case 'Confirmada':
        return 'info';
      case 'No Asistió':
        return 'critical';
      case 'Cancelada':
        return 'neutral';
      case 'Programada':
      default:
        return 'priority';
    }
  }

  getAlertLevelVariant(level: string): 'critical' | 'urgent' | 'priority' | 'stable' {
    const l = (level || '').toLowerCase();
    if (l === 'alta' || l === 'high' || l === 'critica' || l === 'crítica') return 'critical';
    if (l === 'media' || l === 'medium') return 'urgent';
    return 'priority';
  }

  navigateToPatient(identifier?: string | number): void {
    if (identifier) {
      this.router.navigate(['/patients', identifier]);
    }
  }

  startSessionForNextPatient(): void {
    const target = this.nextAppointment?.patientUuid || this.nextAppointment?.patientId;
    if (target) {
      this.router.navigate(['/patients', target], {
        queryParams: { newSession: 'true' }
      });
    }
  }

  getInitials(name?: string): string {
    if (!name) return 'P';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  }
}
