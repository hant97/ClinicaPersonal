import { Component, EventEmitter, Output, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { AppointmentService } from '../../../core/services/appointment.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { Patient } from '../../../core/models/patient.model';
import { Appointment } from '../../../core/models/appointment.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { PatientAutocompleteComponent } from '../../../shared/components/patient-autocomplete/patient-autocomplete.component';

export function futureDateValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    return control.value >= todayStr ? null : { pastDate: true };
  };
}

@Component({
  selector: 'app-appointment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PatientAutocompleteComponent],
  templateUrl: './appointment-form.component.html',
  styleUrls: ['./appointment-form.component.css']
})
export class AppointmentFormComponent implements OnInit {
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
  @Input() appointment: Appointment | null = null;

  appointmentForm!: FormGroup;
  isSubmitting = false;
  
  appointmentModalities: CatalogItem[] = [];

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private patientService: PatientService,
    private catalogService: CatalogService,
    private notificationService: NotificationService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadCatalogs();

    const now = new Date();
    const tzOffset = now.getTimezoneOffset() * 60000;
    const localISO = new Date(now.getTime() - tzOffset).toISOString();
    const today = localISO.split('T')[0];

    this.appointmentForm = this.fb.group({
      patientId: [{ value: this.appointment?.patientId || '', disabled: !!this.appointment }, Validators.required],
      appointmentDate: [this.appointment?.appointmentDate || today, [Validators.required, futureDateValidator()]],
      startTime: [this.appointment?.startTime || '', Validators.required],
      endTime: [this.appointment?.endTime || '', Validators.required],
      status: [this.appointment?.status || 'PROGRAMADA', Validators.required],
      modality: [this.appointment?.modality || 'PRESENCIAL', Validators.required],
      videoCallLink: [this.appointment?.videoCallLink || ''],
      isFirstTime: [this.appointment ? this.appointment.isFirstTime : false],
      notes: [this.appointment?.notes || '', [Validators.maxLength(255)]]
    });

    // Validar requerimiento de link si es virtual
    this.appointmentForm.get('modality')?.valueChanges.subscribe(val => {
      const linkControl = this.appointmentForm.get('videoCallLink');
      if (val === 'VIRTUAL') {
        // Podría ser opcional, el requerimiento dice "opcional si es virtual", así que lo dejamos normal.
      } else {
        linkControl?.setValue('');
      }
    });
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
  }

  onSubmit(): void {
    if (this.appointmentForm.invalid) {
      this.appointmentForm.markAllAsTouched();
      
      // Para depuración:
      Object.keys(this.appointmentForm.controls).forEach(key => {
        const controlErrors = this.appointmentForm.get(key)?.errors;
        if (controlErrors != null) {
          console.error(`Campo ${key} es inválido:`, controlErrors);
        }
      });

      this.toastService.show('Por favor, complete todos los campos obligatorios o corrija los errores (revise que la fecha no sea pasada).', 'error');
      return;
    }

    this.isSubmitting = true;
    const formValue = this.appointmentForm.getRawValue(); // Obtiene incluso los disabled
    const newAppointment: Appointment = {
      ...formValue,
      patientId: Number(formValue.patientId)
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
