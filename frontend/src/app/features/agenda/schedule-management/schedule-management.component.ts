import { Component, OnInit, Output, EventEmitter } from '@angular/core';

import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ProfessionalScheduleService } from '../../../core/services/professional-schedule.service';
import { ScheduleBlockService } from '../../../core/services/schedule-block.service';
import { UserService } from '../../../core/services/user.service';
import { UserProfile } from '../../../core/models/user-profile.model';
import { ScheduleBlock } from '../../../core/models/schedule-block.model';
import { ProfessionalSchedule, WeeklySchedule } from '../../../core/models/professional-schedule.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { FocusTrapDirective } from '../../../shared/directives/focus-trap.directive';
import { LucideAngularModule } from 'lucide-angular';
import {
  X,
  Clock,
  Calendar,
  Plus,
  Trash2,
  Save,
  CheckCircle2,
  AlertTriangle,
  User,
  ShieldAlert,
  CalendarOff
} from '../../../shared/icons/lucide-icons';

interface DayConfig {
  dayOfWeek: number;
  name: string;
  active: boolean;
  startTime: string;
  endTime: string;
}

@Component({
  selector: 'app-schedule-management',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    LucideAngularModule,
    FocusTrapDirective
],
  templateUrl: './schedule-management.component.html'
})
export class ScheduleManagementComponent implements OnInit {
  readonly X = X;
  readonly Clock = Clock;
  readonly Calendar = Calendar;
  readonly Plus = Plus;
  readonly Trash2 = Trash2;
  readonly Save = Save;
  readonly CheckCircle2 = CheckCircle2;
  readonly AlertTriangle = AlertTriangle;
  readonly User = User;
  readonly ShieldAlert = ShieldAlert;
  readonly CalendarOff = CalendarOff;

  @Output() closed = new EventEmitter<void>();
  @Output() updated = new EventEmitter<void>();

  activeTab: 'schedules' | 'blocks' = 'schedules';
  isLoading = false;
  isSaving = false;

  professionals: UserProfile[] = [];
  selectedProfessionalId: number | null = null;

  days: DayConfig[] = [
    { dayOfWeek: 1, name: 'Lunes', active: true, startTime: '09:00', endTime: '18:00' },
    { dayOfWeek: 2, name: 'Martes', active: true, startTime: '09:00', endTime: '18:00' },
    { dayOfWeek: 3, name: 'Miércoles', active: true, startTime: '09:00', endTime: '18:00' },
    { dayOfWeek: 4, name: 'Jueves', active: true, startTime: '09:00', endTime: '18:00' },
    { dayOfWeek: 5, name: 'Viernes', active: true, startTime: '09:00', endTime: '18:00' },
    { dayOfWeek: 6, name: 'Sábado', active: false, startTime: '09:00', endTime: '13:00' },
    { dayOfWeek: 7, name: 'Domingo', active: false, startTime: '09:00', endTime: '13:00' },
  ];

  scheduleForm!: FormGroup;

  // Blocks
  blocks: ScheduleBlock[] = [];
  blockForm!: FormGroup;
  showNewBlockForm = false;
  isSavingBlock = false;

  constructor(
    private fb: FormBuilder,
    private scheduleService: ProfessionalScheduleService,
    private blockService: ScheduleBlockService,
    private userService: UserService,
    private toastService: ToastService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.initScheduleForm();
    this.initBlockForm();
    this.loadProfessionals();
    this.loadBlocks();
  }

  private initScheduleForm(): void {
    const dayControls = this.days.map(d => this.fb.group({
      dayOfWeek: [d.dayOfWeek],
      name: [d.name],
      active: [d.active],
      startTime: [d.startTime, Validators.required],
      endTime: [d.endTime, Validators.required]
    }));

    this.scheduleForm = this.fb.group({
      professionalId: [null, Validators.required],
      days: this.fb.array(dayControls)
    });
  }

  get daysArray(): FormArray {
    return this.scheduleForm.get('days') as FormArray;
  }

  private initBlockForm(): void {
    const today = new Date().toISOString().split('T')[0];
    this.blockForm = this.fb.group({
      professionalId: [null],
      title: ['', [Validators.required, Validators.maxLength(200)]],
      startDate: [today, Validators.required],
      endDate: [today, Validators.required],
      isAllDay: [true],
      startTime: ['09:00'],
      endTime: ['18:00'],
      reason: ['']
    });

    this.blockForm.get('isAllDay')?.valueChanges.subscribe(allDay => {
      const start = this.blockForm.get('startTime');
      const end = this.blockForm.get('endTime');
      if (allDay) {
        start?.clearValidators();
        end?.clearValidators();
      } else {
        start?.setValidators([Validators.required]);
        end?.setValidators([Validators.required]);
      }
      start?.updateValueAndValidity();
      end?.updateValueAndValidity();
    });
  }

  loadProfessionals(): void {
    forkJoin({
      profs: this.userService.getProfessionals(),
      currentUser: this.userService.getCurrentUserProfile().pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ profs, currentUser }) => {
        this.professionals = profs;
        if (profs.length > 0) {
          const defaultProf = currentUser && profs.some(p => p.id === currentUser.id)
            ? currentUser.id
            : profs[0].id;
          this.selectedProfessionalId = defaultProf;
          this.scheduleForm.patchValue({ professionalId: defaultProf });
          this.loadScheduleForProfessional(defaultProf);
        }
      },
      error: (err) => console.error('Error loading professionals', err)
    });
  }

  getProfessionalDisplayName(prof: UserProfile): string {
    const fullName = [prof.firstName, prof.lastName].filter(Boolean).join(' ').trim();
    const name = fullName || prof.username || `Profesional #${prof.id}`;
    return prof.specialty ? `${name} (${prof.specialty})` : name;
  }

  onProfessionalChange(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    if (id) {
      this.selectedProfessionalId = id;
      this.loadScheduleForProfessional(id);
    }
  }

  loadScheduleForProfessional(profId: number): void {
    this.isLoading = true;
    this.scheduleService.getWeeklySchedule(profId).subscribe({
      next: (weekly) => {
        this.isLoading = false;
        if (weekly.schedules && weekly.schedules.length > 0) {
          this.populateScheduleForm(weekly.schedules);
        } else {
          this.resetDefaultScheduleForm();
        }
      },
      error: () => {
        this.isLoading = false;
        this.resetDefaultScheduleForm();
      }
    });
  }

  private populateScheduleForm(schedules: ProfessionalSchedule[]): void {
    const schedulesByDay = new Map<number, ProfessionalSchedule>();
    schedules.forEach(s => schedulesByDay.set(s.dayOfWeek, s));

    this.days.forEach((d, idx) => {
      const existing = schedulesByDay.get(d.dayOfWeek);
      const group = this.daysArray.at(idx) as FormGroup;
      if (existing) {
        group.patchValue({
          active: existing.active,
          startTime: existing.startTime.length >= 5 ? existing.startTime.substring(0, 5) : existing.startTime,
          endTime: existing.endTime.length >= 5 ? existing.endTime.substring(0, 5) : existing.endTime
        });
      } else {
        group.patchValue({ active: false });
      }
    });
  }

  private resetDefaultScheduleForm(): void {
    this.days.forEach((d, idx) => {
      const group = this.daysArray.at(idx) as FormGroup;
      group.patchValue({
        active: d.active,
        startTime: d.startTime,
        endTime: d.endTime
      });
    });
  }

  saveSchedule(): void {
    if (!this.selectedProfessionalId) return;

    const daysVal = this.daysArray.value as any[];
    for (const d of daysVal) {
      if (d.active) {
        if (!d.startTime || !d.endTime) {
          this.toastService.show(`Ingresa horas válidas para el ${d.name}.`, 'error');
          return;
        }
        if (d.startTime >= d.endTime) {
          this.toastService.show(`En ${d.name}, la hora de inicio debe ser anterior a la hora de fin.`, 'error');
          return;
        }
      }
    }

    const payloadSchedules: ProfessionalSchedule[] = daysVal.map(d => ({
      professionalId: this.selectedProfessionalId!,
      dayOfWeek: d.dayOfWeek,
      startTime: d.active ? d.startTime : '09:00',
      endTime: d.active ? d.endTime : '18:00',
      active: d.active
    }));

    const weeklyPayload: WeeklySchedule = {
      professionalId: this.selectedProfessionalId,
      schedules: payloadSchedules
    };

    this.isSaving = true;
    this.scheduleService.saveWeeklySchedule(weeklyPayload).subscribe({
      next: () => {
        this.isSaving = false;
        this.toastService.show('Horario semanal guardado exitosamente.', 'success');
        this.updated.emit();
      },
      error: (err) => {
        this.isSaving = false;
        this.toastService.show(err.error?.message || 'Error al guardar el horario semanal.', 'error');
      }
    });
  }

  // --- BLOCKS LOGIC ---
  loadBlocks(): void {
    this.blockService.getBlocks().subscribe({
      next: (blocks) => {
        this.blocks = blocks;
      },
      error: (err) => console.error('Error loading blocks', err)
    });
  }

  toggleNewBlock(): void {
    this.showNewBlockForm = !this.showNewBlockForm;
  }

  submitBlock(): void {
    if (this.blockForm.invalid) {
      this.blockForm.markAllAsTouched();
      this.toastService.show('Por favor completa todos los campos obligatorios del bloqueo.', 'error');
      return;
    }

    const val = this.blockForm.value;
    if (val.startDate > val.endDate) {
      this.toastService.show('La fecha de inicio no puede ser posterior a la fecha de fin.', 'error');
      return;
    }

    if (!val.isAllDay && val.startTime >= val.endTime) {
      this.toastService.show('La hora de inicio debe ser anterior a la hora de fin.', 'error');
      return;
    }

    const payload: ScheduleBlock = {
      professionalId: val.professionalId ? Number(val.professionalId) : undefined,
      title: val.title.trim(),
      startDate: val.startDate,
      endDate: val.endDate,
      startTime: val.isAllDay ? undefined : val.startTime,
      endTime: val.isAllDay ? undefined : val.endTime,
      reason: val.reason ? val.reason.trim() : undefined
    };

    this.isSavingBlock = true;
    this.blockService.createBlock(payload).subscribe({
      next: () => {
        this.isSavingBlock = false;
        this.showNewBlockForm = false;
        this.blockForm.reset({
          professionalId: null,
          title: '',
          startDate: new Date().toISOString().split('T')[0],
          endDate: new Date().toISOString().split('T')[0],
          isAllDay: true,
          startTime: '09:00',
          endTime: '18:00',
          reason: ''
        });
        this.toastService.show('Bloqueo de agenda registrado correctamente.', 'success');
        this.loadBlocks();
        this.updated.emit();
      },
      error: (err) => {
        this.isSavingBlock = false;
        this.toastService.show(err.error?.message || 'Error al registrar el bloqueo.', 'error');
      }
    });
  }

  async deleteBlock(block: ScheduleBlock): Promise<void> {
    if (!block.id) return;
    const confirmed = await this.notificationService.confirm(
      'Eliminar Bloqueo',
      `¿Deseas eliminar el bloqueo "${block.title}"?`,
      'Sí, eliminar',
      'Cancelar'
    );

    if (confirmed) {
      this.blockService.deleteBlock(block.id).subscribe({
        next: () => {
          this.toastService.show('Bloqueo eliminado correctamente.', 'success');
          this.loadBlocks();
          this.updated.emit();
        },
        error: (err) => {
          this.toastService.show(err.error?.message || 'Error al eliminar el bloqueo.', 'error');
        }
      });
    }
  }

  close(): void {
    this.closed.emit();
  }
}
