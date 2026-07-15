import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';
import { AppointmentService } from '../../../core/services/appointment.service';
import { Appointment } from '../../../core/models/appointment.model';
import { AppointmentFormComponent } from '../appointment-form/appointment-form.component';
import { PatientService } from '../../../core/services/patient/patient.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule, Plus, Calendar, MoreVertical, Check, X, Clock, Video, User, Search, Filter, ChevronLeft, ChevronRight, LayoutList, CalendarDays } from 'lucide-angular';

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [CommonModule, AppointmentFormComponent, LucideAngularModule, ReactiveFormsModule],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.css'
})
export class AgendaComponent implements OnInit, OnDestroy {
  readonly Plus = Plus;
  readonly Calendar = Calendar;
  readonly MoreVertical = MoreVertical;
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

  appointments: Appointment[] = [];
  showForm = false;
  openMenuId: number | null = null;
  appointmentToEdit: Appointment | null = null;
  patientMap = new Map<number, string>();

  filterForm!: FormGroup;
  private destroy$ = new Subject<void>();

  // Calendar State
  currentView: 'list' | 'calendar' = 'list';
  currentWeekStart!: Date;
  weekDays: Date[] = [];
  hours: string[] = [];

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private patientService: PatientService,
    private router: Router,
    private notificationService: NotificationService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.initFilterForm();
    this.initCalendar();

    this.patientService.getAll().subscribe({
      next: (patients) => {
        patients.forEach(p => {
          if (p.id) {
            this.patientMap.set(Number(p.id), `${p.firstName} ${p.lastName}`);
          }
        });
        this.loadAppointments();
      },
      error: (err) => console.error('Error fetching patients', err)
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
        this.loadAppointments();
      });
  }

  // --- CALENDAR LOGIC ---
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
    if (reload && this.currentView === 'calendar') {
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
    this.updateWeekDays(this.currentView === 'calendar');
  }

  toggleView(view: 'list' | 'calendar'): void {
    this.currentView = view;
    if (view === 'calendar') {
      this.filterForm.get('dateRange')?.disable({emitEvent: false});
    } else {
      this.filterForm.get('dateRange')?.enable({emitEvent: false});
    }
    this.loadAppointments();
  }

  // Helper for calendar rendering
  getAppointmentsForDayAndHour(day: Date, hourString: string): Appointment[] {
    // Para asegurar la misma zona horaria y formato ISO local
    const y = day.getFullYear();
    const m = String(day.getMonth() + 1).padStart(2, '0');
    const d = String(day.getDate()).padStart(2, '0');
    const dateStr = `${y}-${m}-${d}`;
    
    const hourPrefix = hourString.substring(0, 2);
    
    return this.appointments.filter(app => {
      return app.appointmentDate === dateStr && app.startTime.startsWith(hourPrefix);
    });
  }

  getAppointmentStyle(app: Appointment): any {
    const startParts = app.startTime.split(':');
    const endParts = app.endTime.split(':');
    
    const startMins = parseInt(startParts[1], 10);
    const startTotalMins = parseInt(startParts[0], 10) * 60 + startMins;
    const endTotalMins = parseInt(endParts[0], 10) * 60 + parseInt(endParts[1], 10);
    const duration = endTotalMins - startTotalMins;
    
    const top = (startMins / 60) * 100;
    const height = (duration / 60) * 100;
    
    return {
      top: `calc(${top}% + 1px)`,
      height: `calc(${height}% - 2px)`,
      position: 'absolute',
      left: '2px',
      right: '2px',
      zIndex: this.openMenuId === app.id ? 100 : 10
    };
  }
  // ----------------------

  loadAppointments(): void {
    const filters = this.filterForm.getRawValue();
    
    let startDate: string | undefined = undefined;
    let endDate: string | undefined = undefined;

    if (this.currentView === 'calendar') {
      // Ajuste timezone local a ISO
      const s = this.weekDays[0];
      startDate = `${s.getFullYear()}-${String(s.getMonth()+1).padStart(2, '0')}-${String(s.getDate()).padStart(2, '0')}`;
      
      const e = this.weekDays[6];
      endDate = `${e.getFullYear()}-${String(e.getMonth()+1).padStart(2, '0')}-${String(e.getDate()).padStart(2, '0')}`;
    } else {
      if (filters.dateRange !== 'ALL') {
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

    this.appointmentService.search(filters.searchTerm, filters.status, startDate, endDate).subscribe({
      next: (data) => {
        this.appointments = data.map(app => ({
          ...app,
          patientName: this.patientMap.get(Number(app.patientId)) || 'Paciente Desconocido'
        }));
      },
      error: (err) => console.error('Error fetching appointments', err)
    });
  }

  openForm(): void {
    this.appointmentToEdit = null;
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.appointmentToEdit = null;
  }

  onAppointmentSaved(): void {
    this.showForm = false;
    this.appointmentToEdit = null;
    this.loadAppointments();
  }

  toggleMenu(id: number | undefined, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    if (!id) return;
    if (this.openMenuId === id) {
      this.openMenuId = null;
    } else {
      this.openMenuId = id;
    }
  }

  // Cierra el menu si se hace clic fuera del popover (requeriria host listener, por ahora cerramos manual)
  closeMenu(): void {
    this.openMenuId = null;
  }

  // --- ACTIONS ---

  confirmAppointment(appointment: Appointment): void {
    this.openMenuId = null;
    this.updateStatus(appointment, 'CONFIRMADA');
  }

  markCompleted(appointment: Appointment): void {
    this.openMenuId = null;
    if (appointment.id) {
      this.appointmentService.updateStatus(appointment.id, 'COMPLETADA').subscribe({
        next: () => {
          this.router.navigate(['/patients', appointment.patientId], { queryParams: { newSession: 'true' } });
        },
        error: (err) => console.error('Error marking as completed', err)
      });
    }
  }

  markNoShow(appointment: Appointment): void {
    this.openMenuId = null;
    this.updateStatus(appointment, 'NO_ASISTIO');
  }

  async cancelAppointment(appointment: Appointment): Promise<void> {
    this.openMenuId = null;
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
    this.openMenuId = null;
    this.appointmentToEdit = appointment;
    this.showForm = true;
  }

  viewPatientProfile(patientId: number): void {
    this.openMenuId = null;
    this.router.navigate(['/patients', patientId]);
  }

  private updateStatus(appointment: Appointment, status: string): void {
    if (appointment.id) {
      this.appointmentService.updateStatus(appointment.id, status).subscribe({
        next: () => {
          this.loadAppointments();
          this.toastService.show(`Cita ${status.toLowerCase()} exitosamente.`, 'success');
        },
        error: (err) => {
          console.error(`Error updating status to ${status}`, err);
          this.toastService.show(err.error?.message || 'Hubo un error al cambiar el estado.', 'error');
        }
      });
    }
  }

  // --- UI HELPERS ---

  getStatusClass(status: string): string {
    switch (status) {
      case 'PROGRAMADA': return 'status-scheduled'; 
      case 'CONFIRMADA': return 'status-confirmed'; 
      case 'COMPLETADA': return 'status-attended'; 
      case 'CANCELADA': return 'status-cancelled'; 
      case 'NO_ASISTIO': return 'status-missed'; 
      default: return '';
    }
  }
}
