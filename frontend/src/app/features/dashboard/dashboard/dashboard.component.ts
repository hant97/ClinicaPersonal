import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { DashboardService, DashboardStats } from '../../../core/services/dashboard.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { RiskAlertService } from '../../../core/services/risk-alert.service';
import { Appointment } from '../../../core/models/appointment.model';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import {
  LucideAngularModule,
  Users,
  Calendar,
  DollarSign,
  Activity,
  UserPlus,
  TrendingUp,
  TrendingDown,
  Clock,
  AlertTriangle,
  Package,
  BrainCircuit,
  Sparkles,
  CheckCircle2,
  ArrowRight,
  ChevronRight,
  Plus,
  Stethoscope,
  FileText,
  Check,
  X,
  AlertCircle,
  Video,
  User,
  Zap,
  Play
} from 'lucide-angular';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule, StatusPillComponent],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit, OnDestroy {
  @ViewChild('trendCanvas') trendCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('attendanceCanvas') attendanceCanvas?: ElementRef<HTMLCanvasElement>;

  stats: DashboardStats | null = null;
  isLoading = true;
  loadError = false;

  isPsychology = false;
  isDermatology = false;
  dashboardTitle = 'Panel Operativo';

  // Next Patient in Queue
  nextAppointment: Appointment | null = null;

  // Kanban Columns
  scheduledList: Appointment[] = [];
  confirmedList: Appointment[] = [];
  inProgressList: Appointment[] = [];
  completedList: Appointment[] = [];

  // Charts
  trendChart: Chart | null = null;
  attendanceChart: Chart | null = null;

  readonly Users = Users;
  readonly Calendar = Calendar;
  readonly DollarSign = DollarSign;
  readonly Activity = Activity;
  readonly UserPlus = UserPlus;
  readonly TrendingUp = TrendingUp;
  readonly TrendingDown = TrendingDown;
  readonly Clock = Clock;
  readonly AlertTriangle = AlertTriangle;
  readonly Package = Package;
  readonly BrainCircuit = BrainCircuit;
  readonly Sparkles = Sparkles;
  readonly CheckCircle2 = CheckCircle2;
  readonly ArrowRight = ArrowRight;
  readonly ChevronRight = ChevronRight;
  readonly Plus = Plus;
  readonly Stethoscope = Stethoscope;
  readonly FileText = FileText;
  readonly Check = Check;
  readonly X = X;
  readonly AlertCircle = AlertCircle;
  readonly Video = Video;
  readonly User = User;
  readonly Zap = Zap;
  readonly Play = Play;

  constructor(
    private dashboardService: DashboardService,
    private specialtyService: SpecialtyService,
    private riskAlertService: RiskAlertService,
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

  ngOnDestroy(): void {
    if (this.trendChart) {
      this.trendChart.destroy();
    }
    if (this.attendanceChart) {
      this.attendanceChart.destroy();
    }
  }

  loadStats(): void {
    this.isLoading = true;
    this.loadError = false;
    this.dashboardService.getDashboardStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.organizeKanban(stats.upcomingAppointments || []);
        this.resolveNextAppointment(stats.upcomingAppointments || []);
        this.isLoading = false;
        setTimeout(() => this.renderCharts(), 50);
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

  private organizeKanban(appointments: Appointment[]): void {
    this.scheduledList = appointments.filter(
      (a) => a.status === 'Programada' || a.status === 'PROGRAMADA'
    );
    this.confirmedList = appointments.filter(
      (a) => a.status === 'Confirmada' || a.status === 'CONFIRMADA'
    );
    this.inProgressList = appointments.filter(
      (a) => a.status === 'En Consulta' || a.status === 'EN_CONSULTA'
    );
    this.completedList = appointments.filter(
      (a) => a.status === 'Completada' || a.status === 'COMPLETADA'
    );
  }

  private renderCharts(): void {
    this.renderTrendChart();
    this.renderAttendanceChart();
  }

  private renderTrendChart(): void {
    if (!this.trendCanvas?.nativeElement || !this.stats) return;

    if (this.trendChart) {
      this.trendChart.destroy();
    }

    const ctx = this.trendCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    // Create subtle gradient for area fill
    const gradient = ctx.createLinearGradient(0, 0, 0, 260);
    gradient.addColorStop(0, 'rgba(37, 99, 235, 0.12)');
    gradient.addColorStop(1, 'rgba(37, 99, 235, 0.00)');

    const labels = ['Semana 1', 'Semana 2', 'Semana 3', 'Semana 4 (Actual)'];
    const totalToday = this.stats.appointmentsToday || 2;
    const active = this.stats.activePatients || 10;
    const dataPoints = [
      Math.max(1, Math.round(active * 0.4)),
      Math.max(2, Math.round(active * 0.65)),
      Math.max(3, Math.round(active * 0.85)),
      Math.max(totalToday, active)
    ];

    this.trendChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Atenciones Clínicas',
          data: dataPoints,
          borderColor: '#2563eb',
          borderWidth: 2,
          backgroundColor: gradient,
          fill: true,
          tension: 0.35,
          pointBackgroundColor: '#2563eb',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#0f172a',
            titleFont: { family: 'Inter', size: 12, weight: 'bold' },
            bodyFont: { family: 'Inter', size: 12 },
            padding: 10,
            cornerRadius: 8
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { font: { family: 'Inter', size: 11 }, color: '#64748b' }
          },
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(226, 232, 240, 0.6)' },
            ticks: { font: { family: 'Inter', size: 11 }, color: '#64748b', precision: 0 }
          }
        }
      }
    });
  }

  private renderAttendanceChart(): void {
    if (!this.attendanceCanvas?.nativeElement || !this.stats) return;

    if (this.attendanceChart) {
      this.attendanceChart.destroy();
    }

    const ctx = this.attendanceCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const rate = this.stats.attendanceRate || 85;
    const cancelled = this.stats.cancelledAppointments || (100 - rate > 0 ? 100 - rate : 15);

    this.attendanceChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Asistencias Efectivas', 'Canceladas / No Asistió'],
        datasets: [{
          data: [rate, cancelled],
          backgroundColor: ['#2563eb', '#e2e8f0'],
          borderWidth: 0,
          hoverOffset: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '76%',
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#0f172a',
            padding: 8,
            cornerRadius: 6
          }
        }
      }
    });
  }

  getAlertLevelVariant(level: string): 'critical' | 'urgent' | 'priority' | 'stable' {
    const l = (level || '').toLowerCase();
    if (l === 'alta' || l === 'high' || l === 'critica' || l === 'crítica') return 'critical';
    if (l === 'media' || l === 'medium') return 'urgent';
    return 'priority';
  }

  navigateToPatient(patientId?: number): void {
    if (patientId) {
      this.router.navigate(['/patients', patientId]);
    }
  }

  startSessionForNextPatient(): void {
    if (this.nextAppointment?.patientId) {
      this.router.navigate(['/patients', this.nextAppointment.patientId], {
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
