import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';
import { AppointmentService } from '../../../core/services/appointment.service';
import { Appointment } from '../../../core/models/appointment.model';
import { AppointmentFormComponent } from '../appointment-form/appointment-form.component';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';
import { DrawerSheetComponent } from '../../../shared/components/drawer-sheet/drawer-sheet.component';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';
import {
  LucideAngularModule,
  Plus,
  Calendar,
  Check,
  X,
  Clock,
  Video,
  User,
  Search,
  Filter,
  ChevronLeft,
  ChevronRight,
  LayoutList,
  CalendarDays,
  Download,
  Eye,
  Stethoscope,
  Receipt,
  AlertTriangle,
  UserX,
  RotateCcw,
  CheckCircle2,
  MoreHorizontal,
  ExternalLink
} from 'lucide-angular';

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    AppointmentFormComponent,
    LucideAngularModule,
    ReactiveFormsModule,
    StatusPillComponent,
    DrawerSheetComponent
  ],
  templateUrl: './agenda.component.html',
})
export class AgendaComponent implements OnInit, OnDestroy {
  readonly Plus = Plus;
  readonly Calendar = Calendar;
  readonly Check = Check;
  readonly X = X;
  readonly Clock = Clock;
  readonly Video = Video;
  readonly User = User;
  readonly Search = Search;
  readonly Filter = Filter;
  readonly ChevronLeft = ChevronLeft;
  readonly ChevronRight = ChevronRight;
  readonly LayoutList = LayoutList;
  readonly CalendarDays = CalendarDays;
  readonly Download = Download;
  readonly Eye = Eye;
  readonly Stethoscope = Stethoscope;
  readonly Receipt = Receipt;
  readonly AlertTriangle = AlertTriangle;
  readonly UserX = UserX;
  readonly RotateCcw = RotateCcw;
  readonly CheckCircle2 = CheckCircle2;
  readonly MoreHorizontal = MoreHorizontal;
  readonly ExternalLink = ExternalLink;

  appointments: Appointment[] = [];
  showForm = false;
  appointmentToEdit: Appointment | null = null;
  initialAppointmentData: Partial<Appointment> | null = null;
  isLoading = false;
  loadError = false;
  openMenuAppointmentId: number | null = null;

  // Quick Drawer Preview State
  selectedAppointmentPreview: Appointment | null = null;
  isDrawerOpen = false;
  isUpdatingStatus = false;
  updatingStatusId: number | null = null;
  activeStatusAction: string | null = null;

  filterForm!: FormGroup;
  private destroy$ = new Subject<void>();

  // View States
  currentView: 'calendar' | 'list' = 'calendar';
  currentWeekStart!: Date;
  weekDays: Date[] = [];
  hours: string[] = [];
  listSpecificDate: string | null = null;
  todayCount = 0;

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private exportService: ExportService,
    private viewPreferenceService: ViewPreferenceService
  ) {}

  ngOnInit(): void {
    this.currentView = this.viewPreferenceService.getViewMode<'calendar' | 'list'>('agenda_view_mode', 'calendar', 'list');
    this.initFilterForm();
    this.initCalendar();
    this.loadTodayCount();

    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['newAppointment'] === 'true' || params['patientId']) {
        const patientId = params['patientId'] ? Number(params['patientId']) : undefined;
        this.openForm(undefined, {
          patientId: patientId
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initFilterForm(): void {
    this.filterForm = this.fb.group({
      searchTerm: [''],
      status: ['ALL'],
      dateRange: ['ALL']
    });

    this.filterForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.listSpecificDate = null;
        this.loadAppointments();
      });
  }

  // --- CALENDAR & TIMELINE LOGIC ---
  private initCalendar(): void {
    for (let i = 8; i <= 20; i++) {
      this.hours.push(i.toString().padStart(2, '0') + ':00');
    }
    this.todayWeek();
  }

  getStartOfWeek(date: Date): Date {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Lunes
    return new Date(d.setDate(diff));
  }

  isToday(day: Date): boolean {
    const today = new Date();
    return day.getDate() === today.getDate() &&
           day.getMonth() === today.getMonth() &&
           day.getFullYear() === today.getFullYear();
  }

  updateWeekDays(reload = true): void {
    this.weekDays = [];
    for(let i = 0; i < 7; i++) {
      const day = new Date(this.currentWeekStart);
      day.setDate(this.currentWeekStart.getDate() + i);
      this.weekDays.push(day);
    }
    if (reload) {
      this.loadAppointments();
    }
  }

  prevWeek(): void {
    this.currentWeekStart.setDate(this.currentWeekStart.getDate() - 7);
    this.updateWeekDays();
  }

  nextWeek(): void {
    this.currentWeekStart.setDate(this.currentWeekStart.getDate() + 7);
    this.updateWeekDays();
  }

  todayWeek(): void {
    this.currentWeekStart = this.getStartOfWeek(new Date());
    this.updateWeekDays(true);
  }

  showDayInList(day: Date): void {
    this.listSpecificDate = this.toDateKey(day);
    this.currentView = 'list';
    this.loadAppointments();
  }

  clearSpecificDate(): void {
    this.listSpecificDate = null;
    this.loadAppointments();
  }

  private toDateKey(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  private loadTodayCount(): void {
    const key = this.toDateKey(new Date());
    this.appointmentService.search(undefined, 'ALL', key, key).subscribe({
      next: (page) => { this.todayCount = page.content.length; },
      error: () => {}
    });
  }

  toggleView(view: 'calendar' | 'list'): void {
    this.currentView = view;
    this.viewPreferenceService.setViewMode('agenda_view_mode', view);
    this.listSpecificDate = null;
    this.loadAppointments();
  }

  getAppointmentsForDayAndHour(day: Date, hourString: string): Appointment[] {
    const dateStr = this.toDateKey(day);
    const hourPrefix = hourString.substring(0, 2);
    return this.appointments.filter(app => {
      return app.appointmentDate === dateStr && app.startTime.startsWith(hourPrefix);
    });
  }

  loadAppointments(): void {
    const filters = this.filterForm.getRawValue();
    
    let startDate: string | undefined = undefined;
    let endDate: string | undefined = undefined;

    if (this.currentView === 'calendar') {
      const s = this.weekDays[0] || this.currentWeekStart;
      startDate = this.toDateKey(s);
      const e = this.weekDays[6] || s;
      endDate = this.toDateKey(e);
    } else {
      if (this.listSpecificDate) {
        startDate = this.listSpecificDate;
        endDate = this.listSpecificDate;
      } else if (filters.dateRange !== 'ALL') {
        const today = new Date();
        const ty = today.getFullYear();
        const tm = String(today.getMonth()+1).padStart(2, '0');
        const td = String(today.getDate()).padStart(2, '0');
        
        if (filters.dateRange === 'TODAY') {
          startDate = `${ty}-${tm}-${td}`;
          endDate = startDate;
        } else if (filters.dateRange === 'WEEK') {
          const firstDay = this.getStartOfWeek(new Date());
          const lastDay = new Date(firstDay);
          lastDay.setDate(firstDay.getDate() + 6);
          startDate = `${firstDay.getFullYear()}-${String(firstDay.getMonth()+1).padStart(2, '0')}-${String(firstDay.getDate()).padStart(2, '0')}`;
          endDate = `${lastDay.getFullYear()}-${String(lastDay.getMonth()+1).padStart(2, '0')}-${String(lastDay.getDate()).padStart(2, '0')}`;
        } else if (filters.dateRange === 'MONTH') {
          startDate = `${ty}-${tm}-01`;
          const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
          endDate = `${ty}-${tm}-${String(lastDay.getDate()).padStart(2, '0')}`;
        }
      }
    }

    this.isLoading = true;
    this.loadError = false;
    this.appointmentService.search(filters.searchTerm, filters.status, startDate, endDate).subscribe({
      next: (data) => {
        this.appointments = data.content;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error fetching appointments', err);
        this.isLoading = false;
        this.loadError = true;
      }
    });
  }

  // Quick Preview Drawer
  openPreview(app: Appointment, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.selectedAppointmentPreview = app;
    this.isDrawerOpen = true;
  }

  closePreview(): void {
    this.isDrawerOpen = false;
    this.selectedAppointmentPreview = null;
  }

  openForm(appointment?: Appointment, initialData?: Partial<Appointment>): void {
    this.appointmentToEdit = appointment || null;
    this.initialAppointmentData = initialData || null;
    this.showForm = true;
    this.closePreview();
  }

  onSlotClick(day: Date, hourString: string): void {
    const dateStr = this.toDateKey(day);

    const startTime = hourString.length === 5 ? hourString : hourString.substring(0, 5);
    const [h, min] = startTime.split(':').map(Number);
    const endMinutes = h * 60 + min + 30;
    const endH = Math.floor(endMinutes / 60);
    const endM = endMinutes % 60;
    const endTime = `${String(endH).padStart(2, '0')}:${String(endM).padStart(2, '0')}`;

    this.appointmentToEdit = null;
    this.initialAppointmentData = {
      appointmentDate: dateStr,
      startTime: startTime,
      endTime: endTime
    };
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.appointmentToEdit = null;
    this.initialAppointmentData = null;
  }

  onAppointmentSaved(): void {
    this.showForm = false;
    this.appointmentToEdit = null;
    this.initialAppointmentData = null;
    this.loadAppointments();
  }

  // --- ACTIONS ---

  confirmAppointment(appointment: Appointment): void {
    this.updateStatus(appointment, 'CONFIRMADA');
  }

  registerConsultation(appointment: Appointment): void {
    const patientKey = appointment.patientUuid || appointment.patientId;
    this.router.navigate(['/patients', patientKey, 'sessions', 'new'], {
      queryParams: {
        appointmentId: appointment.id,
        date: appointment.appointmentDate,
        startTime: appointment.startTime,
        endTime: appointment.endTime,
        modality: appointment.modality,
        service: appointment.clinicalServiceName
      }
    });
  }

  viewConsultation(appointment: Appointment): void {
    const patientKey = appointment.patientUuid || appointment.patientId;
    if (appointment.clinicalSessionId) {
      this.router.navigate(['/patients', patientKey, 'sessions', appointment.clinicalSessionId, 'edit']);
    } else {
      this.registerConsultation(appointment);
    }
  }

  reopenAppointment(appointment: Appointment): void {
    this.updateStatus(appointment, 'CONFIRMADA');
  }

  markNoShow(appointment: Appointment): void {
    this.updateStatus(appointment, 'NO_ASISTIO');
  }

  async cancelAppointment(appointment: Appointment): Promise<void> {
    const confirmed = await this.notificationService.confirm(
      'Cancelar Cita',
      '¿Estás seguro de cancelar esta cita? Esta acción no se puede deshacer.',
      'Sí, cancelar',
      'No, mantener'
    );
    if (confirmed) {
      this.updateStatus(appointment, 'CANCELADA');
    }
  }

  openRescheduleForm(appointment: Appointment): void {
    this.appointmentToEdit = appointment;
    this.initialAppointmentData = null;
    this.showForm = true;
  }

  viewPatientProfile(identifier: string | number): void {
    this.closePreview();
    this.router.navigate(['/patients', identifier]);
  }

  goToPaymentDetail(paymentId?: number): void {
    this.closePreview();
    if (paymentId) {
      this.router.navigate(['/billing'], {
        queryParams: { paymentId: paymentId }
      });
    } else {
      this.router.navigate(['/billing']);
    }
  }

  goToBilling(appointment: Appointment): void {
    this.closePreview();
    this.router.navigate(['/billing'], {
      queryParams: {
        newPayment: 'true',
        patientId: appointment.patientId,
        appointmentId: appointment.id,
        clinicalServiceId: appointment.clinicalServiceId,
        description: `Cobro de cita del ${appointment.appointmentDate} ${appointment.startTime}`
      }
    });
  }

  private updateStatus(appointment: Appointment, status: string): void {
    if (appointment.id) {
      this.isUpdatingStatus = true;
      this.updatingStatusId = appointment.id;
      this.activeStatusAction = status;

      this.appointmentService.updateStatus(appointment.id, status).subscribe({
        next: (updated) => {
          this.isUpdatingStatus = false;
          this.updatingStatusId = null;
          this.activeStatusAction = null;
          this.loadAppointments();
          if (this.selectedAppointmentPreview && this.selectedAppointmentPreview.id === appointment.id) {
            this.selectedAppointmentPreview = { ...this.selectedAppointmentPreview, ...updated, status: status };
          }
          const syncNote = (status === 'CONFIRMADA' && updated.googleEventLink) ? ' y sincronizada con Google Calendar' : '';
          this.toastService.show(`Estado actualizado: ${this.getStatusLabel(status)}${syncNote}.`, 'success');
        },
        error: (err) => {
          this.isUpdatingStatus = false;
          this.updatingStatusId = null;
          this.activeStatusAction = null;
          console.error(`Error updating status to ${status}`, err);
          this.toastService.show(err.error?.message || 'Hubo un error al cambiar el estado.', 'error');
        }
      });
    }
  }

  getPatientInitials(name?: string): string {
    if (!name) return 'P';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  }

  getStatusPillVariant(status: string): 'critical' | 'urgent' | 'priority' | 'stable' | 'neutral' {
    const s = (status || '').toUpperCase();
    if (s === 'CONFIRMADA') return 'priority';
    if (s === 'PROGRAMADA') return 'urgent';
    if (s === 'COMPLETADA') return 'stable';
    if (s === 'NO_ASISTIO' || s === 'CANCELADA') return 'critical';
    return 'neutral';
  }

  getStatusLabel(status: string): string {
    switch ((status || '').toUpperCase()) {
      case 'PROGRAMADA': return 'Programada';
      case 'CONFIRMADA': return 'Confirmada';
      case 'COMPLETADA': return 'Completada';
      case 'NO_ASISTIO': return 'No asistió';
      case 'CANCELADA': return 'Cancelada';
      default: return status || '';
    }
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.openMenuAppointmentId = null;
  }

  toggleMenu(appointmentId?: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    if (!appointmentId) return;
    this.openMenuAppointmentId = this.openMenuAppointmentId === appointmentId ? null : appointmentId;
  }

  closeMenu(): void {
    this.openMenuAppointmentId = null;
  }

  formatTimeRange(start?: string, end?: string): string {
    if (!start) return '';
    const s = start.length >= 5 ? start.substring(0, 5) : start;
    const e = end ? (end.length >= 5 ? end.substring(0, 5) : end) : '';
    return e ? `${s} – ${e}` : s;
  }

  getStatusDotClass(status: string): string {
    const s = (status || '').toUpperCase();
    switch (s) {
      case 'PROGRAMADA': return 'bg-amber-500';
      case 'CONFIRMADA': return 'bg-blue-500';
      case 'COMPLETADA': return 'bg-emerald-500';
      case 'CANCELADA': return 'bg-red-500';
      case 'NO_ASISTIO': return 'bg-rose-400';
      default: return 'bg-slate-400';
    }
  }

  exportAppointments(): void {
    const dataToExport = this.appointments.map(app => ({
      'Paciente': app.patientName,
      'Fecha': (app.appointmentDate || '').replace('T', ' '),
      'Hora Inicio': app.startTime,
      'Hora Fin': app.endTime,
      'Motivo': app.notes || '',
      'Estado': this.getStatusLabel(app.status),
      'Tipo': app.modality === 'VIRTUAL' ? 'Virtual' : 'Presencial'
    }));

    this.exportService.exportToExcel(dataToExport, 'Citas_Agenda');
  }
}
