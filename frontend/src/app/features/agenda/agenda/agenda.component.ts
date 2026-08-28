import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { COMMON_STANDALONE_IMPORTS } from '../../../shared/common-standalone-imports';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';
import { AgendaDataService } from './agenda-data.service';
import { Appointment } from '../../../core/models/appointment.model';
import { ScheduleBlock } from '../../../core/models/schedule-block.model';
import { UserProfile } from '../../../core/models/user-profile.model';
import { AppointmentFormComponent } from '../appointment-form/appointment-form.component';
import { ScheduleManagementComponent } from '../schedule-management/schedule-management.component';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';
import { DrawerSheetComponent } from '../../../shared/components/drawer-sheet/drawer-sheet.component';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';
import { ClinicSettingsService } from '../../../core/services/clinic-settings.service';
import { AgendaToolbarComponent } from '../agenda-toolbar/agenda-toolbar.component';
import {
  toDateKey,
  getStartOfWeek,
  isToday as isTodayUtil,
  computeAppointmentsDateRange,
  computeSlotTimeRange,
  getPatientInitials as getPatientInitialsUtil,
  getStatusPillVariant as getStatusPillVariantUtil,
  getStatusLabel as getStatusLabelUtil,
  getStatusDotClass as getStatusDotClassUtil,
  formatTimeRange as formatTimeRangeUtil,
  whatsAppLink as whatsAppLinkUtil,
  mapAppointmentsForExport,
  StatusPillVariant
} from './agenda.utils';
import { LucideAngularModule } from 'lucide-angular';
import {
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
  ExternalLink,
  MessageCircle,
  BellRing,
  Repeat,
  CalendarOff,
  Settings
} from '../../../shared/icons/lucide-icons';

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [
    ...COMMON_STANDALONE_IMPORTS,
    RouterModule,
    AppointmentFormComponent,
    ScheduleManagementComponent,
    LucideAngularModule,
    StatusPillComponent,
    DrawerSheetComponent,
    AgendaToolbarComponent
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
  readonly MessageCircle = MessageCircle;
  readonly BellRing = BellRing;
  readonly Repeat = Repeat;
  readonly CalendarOff = CalendarOff;
  readonly Settings = Settings;

  appointments: Appointment[] = [];
  scheduleBlocks: ScheduleBlock[] = [];
  professionals: UserProfile[] = [];

  showForm = false;
  showScheduleModal = false;
  appointmentToEdit: Appointment | null = null;
  initialAppointmentData: Partial<Appointment> | null = null;
  isLoading = false;
  loadError = false;
  openMenuAppointmentId: number | null = null;

  // Quick Drawer Preview State
  selectedAppointmentPreview: Appointment | null = null;
  isDrawerOpen = false;
  isUpdatingStatus = false;
  isStartingAttention = false;
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
    private dataService: AgendaDataService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private exportService: ExportService,
    private viewPreferenceService: ViewPreferenceService,
    private clinicSettingsService: ClinicSettingsService
  ) {}

  ngOnInit(): void {
    this.currentView = this.viewPreferenceService.getViewMode<'calendar' | 'list'>('agenda_view_mode', 'calendar', 'list');
    this.initFilterForm();
    this.initCalendar();
    this.loadProfessionals();
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
      professionalId: ['ALL'],
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

  loadProfessionals(): void {
    this.dataService.loadProfessionals().subscribe({
      next: (profs) => {
        this.professionals = profs;
      },
      error: (err) => {
        console.error('Error loading professionals', err);
        this.toastService.show('No se pudieron cargar los profesionales', 'error');
      }
    });
  }

  getProfessionalDisplayName(prof: UserProfile): string {
    const fullName = [prof.firstName, prof.lastName].filter(Boolean).join(' ').trim();
    const name = fullName || prof.username || `Profesional #${prof.id}`;
    return prof.specialty ? `${name} (${prof.specialty})` : name;
  }

  // --- CALENDAR & TIMELINE LOGIC ---
  private initCalendar(): void {
    for (let i = 8; i <= 20; i++) {
      this.hours.push(i.toString().padStart(2, '0') + ':00');
    }
    this.todayWeek();
  }

  getStartOfWeek(date: Date): Date {
    return getStartOfWeek(date);
  }

  isToday(day: Date): boolean {
    return isTodayUtil(day);
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
    this.currentWeekStart = getStartOfWeek(new Date());
    this.updateWeekDays(true);
  }

  showDayInList(day: Date): void {
    this.listSpecificDate = toDateKey(day);
    this.currentView = 'list';
    this.loadAppointments();
  }

  clearSpecificDate(): void {
    this.listSpecificDate = null;
    this.loadAppointments();
  }

  private loadTodayCount(): void {
    const key = toDateKey(new Date());
    this.dataService.countAppointmentsOnDate(key).subscribe({
      next: (count) => { this.todayCount = count; },
      error: () => {
        this.todayCount = 0;
      }
    });
  }

  toggleView(view: 'calendar' | 'list'): void {
    this.currentView = view;
    this.viewPreferenceService.setViewMode('agenda_view_mode', view);
    this.listSpecificDate = null;
    this.loadAppointments();
  }

  getAppointmentsForDayAndHour(day: Date, hourString: string): Appointment[] {
    const dateStr = toDateKey(day);
    const hourPrefix = hourString.substring(0, 2);
    return this.appointments.filter(app => {
      return app.appointmentDate === dateStr && app.startTime.startsWith(hourPrefix);
    });
  }

  getBlocksForDay(day: Date): ScheduleBlock[] {
    const dateStr = toDateKey(day);
    return this.scheduleBlocks.filter(b => b.startDate <= dateStr && b.endDate >= dateStr);
  }

  loadAppointments(): void {
    const filters = this.filterForm.getRawValue();
    const professionalId = filters.professionalId && filters.professionalId !== 'ALL'
      ? Number(filters.professionalId)
      : undefined;

    const { startDate, endDate } = computeAppointmentsDateRange({
      currentView: this.currentView,
      weekDays: this.weekDays,
      currentWeekStart: this.currentWeekStart,
      listSpecificDate: this.listSpecificDate,
      dateRangeFilter: filters.dateRange
    });

    this.isLoading = true;
    this.loadError = false;
    this.dataService.searchAppointments(filters.searchTerm, filters.status, startDate, endDate, professionalId).subscribe({
      next: (appointments) => {
        this.appointments = appointments;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error fetching appointments', err);
        this.isLoading = false;
        this.loadError = true;
      }
    });

    // Load blocks in range
    this.dataService.loadBlocks(startDate, endDate, professionalId).subscribe({
      next: (blocks) => {
        this.scheduleBlocks = blocks;
      },
      error: (err) => {
        console.error('Error fetching schedule blocks', err);
        this.toastService.show('No se pudieron cargar los bloqueos de agenda', 'error');
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

  openScheduleModal(): void {
    this.showScheduleModal = true;
  }

  closeScheduleModal(): void {
    this.showScheduleModal = false;
  }

  onScheduleUpdated(): void {
    this.loadAppointments();
  }

  onSlotClick(day: Date, hourString: string): void {
    const dateStr = toDateKey(day);
    const { startTime, endTime } = computeSlotTimeRange(hourString);

    const filters = this.filterForm.getRawValue();
    const profId = filters.professionalId && filters.professionalId !== 'ALL' ? Number(filters.professionalId) : undefined;

    this.appointmentToEdit = null;
    this.initialAppointmentData = {
      appointmentDate: dateStr,
      startTime: startTime,
      endTime: endTime,
      professionalId: profId
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

  startAttention(appointment: Appointment): void {
    if (!appointment.id || this.isStartingAttention || this.isUpdatingStatus) return;
    this.isStartingAttention = true;
    this.dataService.startAttentionFromAppointment(appointment.id).subscribe({
      next: () => {
        this.isStartingAttention = false;
        this.toastService.show('Atención iniciada desde la cita', 'success');
        this.closePreview();
        this.router.navigate(['/attentions']);
      },
      error: () => {
        this.isStartingAttention = false;
        this.toastService.show('Error al iniciar la atención', 'error');
      }
    });
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
    if (appointment.recurrenceGroupId) {
      const isSeriesCancel = await this.notificationService.confirm(
        'Cancelar Cita Recurrente',
        'Esta cita forma parte de una serie periódica. ¿Deseas cancelar todas las citas futuras de esta serie o únicamente esta sesión?',
        'Cancelar Toda la Serie',
        'Cancelar Solo Esta Sesión'
      );
      this.updateStatus(appointment, 'CANCELADA', isSeriesCancel);
    } else {
      const confirmed = await this.notificationService.confirm(
        'Cancelar Cita',
        '¿Estás seguro de cancelar esta cita? Esta acción no se puede deshacer.',
        'Sí, cancelar',
        'No, mantener'
      );
      if (confirmed) {
        this.updateStatus(appointment, 'CANCELADA', false);
      }
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

  private updateStatus(appointment: Appointment, status: string, updateSeries: boolean = false): void {
    if (appointment.id) {
      this.isUpdatingStatus = true;
      this.updatingStatusId = appointment.id;
      this.activeStatusAction = status;

      this.dataService.updateAppointmentStatus(appointment.id, status, updateSeries).subscribe({
        next: (updated) => {
          this.isUpdatingStatus = false;
          this.updatingStatusId = null;
          this.activeStatusAction = null;
          this.loadAppointments();
          if (this.selectedAppointmentPreview && this.selectedAppointmentPreview.id === appointment.id) {
            this.selectedAppointmentPreview = { ...this.selectedAppointmentPreview, ...updated, status: status };
          }
          const syncNote = (status === 'CONFIRMADA' && updated.googleEventLink) ? ' y sincronizada con Google Calendar' : '';
          const seriesNote = updateSeries ? ' (serie completa)' : '';
          this.toastService.show(`Estado actualizado: ${this.getStatusLabel(status)}${seriesNote}${syncNote}.`, 'success');
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
    return getPatientInitialsUtil(name);
  }

  getStatusPillVariant(status: string): StatusPillVariant {
    return getStatusPillVariantUtil(status);
  }

  getStatusLabel(status: string): string {
    return getStatusLabelUtil(status);
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
    return formatTimeRangeUtil(start, end);
  }

  whatsAppLink(app: Appointment): string | null {
    return whatsAppLinkUtil(app, this.clinicSettingsService.getSnapshotClinicName());
  }

  getStatusDotClass(status: string): string {
    return getStatusDotClassUtil(status);
  }

  exportAppointments(): void {
    this.exportService.exportToCsv(mapAppointmentsForExport(this.appointments), 'Citas_Agenda');
  }
}
