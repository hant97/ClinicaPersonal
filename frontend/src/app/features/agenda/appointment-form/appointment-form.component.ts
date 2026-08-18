import { Component, EventEmitter, Output, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { AppointmentService } from '../../../core/services/appointment.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
import { Appointment } from '../../../core/models/appointment.model';
import { ClinicalService } from '../../../core/models/clinical-service.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { PatientAutocompleteComponent } from '../../../shared/components/patient-autocomplete/patient-autocomplete.component';
import { LucideAngularModule, Clock, AlertTriangle, X, Calendar, User, Save, Video } from 'lucide-angular';
import { FocusTrapDirective } from '../../../shared/directives/focus-trap.directive';

export function futureDateValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    return control.value >= todayStr ? null : { pastDate: true };
  };
}

export function timeOrderValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const start = control.get('startTime')?.value;
    const end = control.get('endTime')?.value;
    if (!start || !end) return null;
    return end > start ? null : { invalidTimeOrder: true };
  };
}

@Component({
  selector: 'app-appointment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PatientAutocompleteComponent, LucideAngularModule, FocusTrapDirective],
  templateUrl: './appointment-form.component.html',
})
export class AppointmentFormComponent implements OnInit {
  readonly Clock = Clock;
  readonly AlertTriangle = AlertTriangle;
  readonly X = X;
  readonly Calendar = Calendar;
  readonly User = User;
  readonly Save = Save;
  readonly Video = Video;

  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
  @Input() appointment: Appointment | null = null;
  @Input() initialData: Partial<Appointment> | null = null;

  appointmentForm!: FormGroup;
  isSubmitting = false;
  
  appointmentModalities: CatalogItem[] = [];
  clinicalServices: ClinicalService[] = [];
  dayAppointments: Appointment[] = [];
  conflicts: Appointment[] = [];

  durationOptions = [
    { label: '15 min', value: 15 },
    { label: '30 min', value: 30 },
    { label: '45 min', value: 45 },
    { label: '1 hora', value: 60 },
    { label: '1.5 h', value: 90 },
  ];
  selectedDuration: number | null = 30;

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private catalogService: CatalogService,
    private clinicalServiceService: ClinicalServiceService,
    private notificationService: NotificationService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadCatalogs();

    const now = new Date();
    const tzOffset = now.getTimezoneOffset() * 60000;
    const localISO = new Date(now.getTime() - tzOffset).toISOString();
    const today = localISO.split('T')[0];

    const initialDate = this.appointment?.appointmentDate || this.initialData?.appointmentDate || today;
    const initialStartTime = this.appointment?.startTime || this.initialData?.startTime || '';
    const initialEndTime = this.appointment?.endTime || this.initialData?.endTime || '';
    const initialPatientId = this.appointment?.patientId || this.initialData?.patientId || '';

    if (initialStartTime && initialEndTime) {
      this.selectedDuration = this.calculateDurationFromTimes(initialStartTime, initialEndTime);
    } else {
      this.selectedDuration = 30;
    }

    this.appointmentForm = this.fb.group({
      patientId: [{ value: initialPatientId, disabled: !!this.appointment }, Validators.required],
      appointmentDate: [initialDate, [Validators.required, futureDateValidator()]],
      startTime: [initialStartTime, Validators.required],
      endTime: [initialEndTime, Validators.required],
      status: [this.appointment?.status || 'PROGRAMADA', Validators.required],
      modality: [this.appointment?.modality || 'PRESENCIAL', Validators.required],
      videoCallLink: [this.appointment?.videoCallLink || ''],
      clinicalServiceId: [this.appointment?.clinicalServiceId || ''],
      isFirstTime: [this.appointment ? this.appointment.isFirstTime : false],
      notes: [this.appointment?.notes || '', [Validators.maxLength(255)]]
    }, { validators: [timeOrderValidator()] });

    // Cargar citas del día inicial para validación de colisiones
    this.loadDayAppointments(initialDate);

    // Reaccionar a cambios en fecha
    this.appointmentForm.get('appointmentDate')?.valueChanges.subscribe(date => {
      if (date) {
        this.loadDayAppointments(date);
      }
    });

    // Reactively compute endTime when startTime changes
    this.appointmentForm.get('startTime')?.valueChanges.subscribe(val => {
      if (val && this.selectedDuration) {
        const newEndTime = this.addMinutesToTime(val, this.selectedDuration);
        this.appointmentForm.patchValue({ endTime: newEndTime }, { emitEvent: false });
        this.appointmentForm.updateValueAndValidity();
      } else if (val && this.appointmentForm.get('endTime')?.value) {
        this.selectedDuration = this.calculateDurationFromTimes(val, this.appointmentForm.get('endTime')?.value);
      }
      this.checkCollisions();
    });

    // Detect manual changes in endTime to update chip selection
    this.appointmentForm.get('endTime')?.valueChanges.subscribe(val => {
      const start = this.appointmentForm.get('startTime')?.value;
      if (start && val) {
        this.selectedDuration = this.calculateDurationFromTimes(start, val);
      }
      this.checkCollisions();
    });

    // Validar requerimiento de link si es virtual
    this.appointmentForm.get('modality')?.valueChanges.subscribe(val => {
      const linkControl = this.appointmentForm.get('videoCallLink');
      if (val === 'VIRTUAL') {
        // Opcional si es virtual
      } else {
        linkControl?.setValue('');
      }
    });
  }

  loadDayAppointments(dateStr: string): void {
    if (!dateStr) return;
    this.appointmentService.search(undefined, 'ALL', dateStr, dateStr).subscribe({
      next: (page) => {
        this.dayAppointments = page.content.filter(app => 
          app.status !== 'CANCELADA' && 
          (!this.appointment || app.id !== this.appointment.id)
        );
        this.checkCollisions();
      },
      error: (err) => console.error('Error loading day appointments', err)
    });
  }

  checkCollisions(): void {
    const start = this.appointmentForm.get('startTime')?.value;
    const end = this.appointmentForm.get('endTime')?.value;
    if (!start || !end || start >= end) {
      this.conflicts = [];
      return;
    }

    const startNorm = start.length === 5 ? start + ':00' : start;
    const endNorm = end.length === 5 ? end + ':00' : end;

    this.conflicts = this.dayAppointments.filter(app => {
      const appStart = app.startTime.length === 5 ? app.startTime + ':00' : app.startTime;
      const appEnd = app.endTime.length === 5 ? app.endTime + ':00' : app.endTime;
      // Overlap condition: startNorm < appEnd && appStart < endNorm
      return startNorm < appEnd && appStart < endNorm;
    });
  }

  selectDuration(minutes: number): void {
    this.selectedDuration = minutes;
    const start = this.appointmentForm.get('startTime')?.value;
    if (start) {
      const newEndTime = this.addMinutesToTime(start, minutes);
      this.appointmentForm.patchValue({ endTime: newEndTime }, { emitEvent: false });
      this.appointmentForm.updateValueAndValidity();
      this.checkCollisions();
    }
  }

  private calculateDurationFromTimes(start: string, end: string): number | null {
    if (!start || !end) return null;
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    const diff = (eh * 60 + em) - (sh * 60 + sm);
    return diff > 0 ? diff : null;
  }

  private addMinutesToTime(start: string, minutes: number): string {
    const [sh, sm] = start.split(':').map(Number);
    const total = sh * 60 + sm + minutes;
    const h = Math.floor(total / 60) % 24;
    const m = total % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  }

  loadCatalogs(): void {
    this.catalogService.getActiveItemsByCatalogCode('APPOINTMENT_MODALITY').subscribe({
      next: (items) => {
        this.appointmentModalities = items;
      },
      error: () => {
        this.toastService.show('Error al cargar modalidades', 'error');
      }
    });

    this.clinicalServiceService.getAllActiveServices().subscribe({
      next: (services) => {
        this.clinicalServices = services;
      },
      error: () => {
        console.error('Error loading clinical services', new Error());
      }
    });
  }

  onServiceSelect(event: Event): void {
    const serviceId = (event.target as HTMLSelectElement).value;
    const service = this.clinicalServices.find(s => s.id === Number(serviceId));
    if (service?.durationMinutes) {
      this.selectDuration(service.durationMinutes);
    }
  }

  onSubmit(): void {
    if (this.appointmentForm.invalid) {
      this.appointmentForm.markAllAsTouched();
      
      if (this.appointmentForm.hasError('invalidTimeOrder')) {
        this.toastService.show('La hora de fin debe ser posterior a la hora de inicio.', 'error');
        return;
      }

      this.toastService.show('Por favor, complete todos los campos obligatorios o corrija los errores (revise que la fecha no sea pasada).', 'error');
      return;
    }

    if (this.conflicts.length > 0) {
      this.toastService.show('Existe un conflicto de horario con otra cita en este horario.', 'error');
      return;
    }

    this.isSubmitting = true;
    const formValue = this.appointmentForm.getRawValue();
    const newAppointment: Appointment = {
      ...formValue,
      patientId: Number(formValue.patientId),
      clinicalServiceId: formValue.clinicalServiceId ? Number(formValue.clinicalServiceId) : null
    };
    
    const request$ = this.appointment ? 
      this.appointmentService.update(this.appointment.id!, newAppointment) : 
      this.appointmentService.create(newAppointment);

    request$.subscribe({
      next: () => {
        this.isSubmitting = false;
        this.saved.emit();
      },
      error: (err) => {
        console.error('Error saving appointment', err);
        this.isSubmitting = false;
        this.notificationService.alert('Error al guardar', err.error?.message || 'Ocurrió un error al guardar la cita (verifique conflictos de horario).', 'error');
      }
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
