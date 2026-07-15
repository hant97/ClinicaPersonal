import { Component, EventEmitter, Output, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppointmentService } from '../../../core/services/appointment.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { Appointment } from '../../../core/models/appointment.model';

import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function futureDateValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const selectedDate = new Date(control.value);
    const now = new Date();
    now.setMinutes(now.getMinutes() - 10); // Allow slight delay
    return selectedDate >= now ? null : { pastDate: true };
  };
}

@Component({
  selector: 'app-appointment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './appointment-form.component.html',
  styleUrls: ['./appointment-form.component.css']
})
export class AppointmentFormComponent implements OnInit {
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
  @Input() appointment: Appointment | null = null;

  appointmentForm!: FormGroup;
  isSubmitting = false;
  patients: Patient[] = [];

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.appointmentForm = this.fb.group({
      patientId: [{ value: this.appointment?.patientId || '', disabled: !!this.appointment }, Validators.required],
      appointmentDate: [
        this.appointment?.appointmentDate 
          ? new Date(this.appointment.appointmentDate).toISOString().substring(0, 16) 
          : new Date().toISOString().substring(0, 16), 
        [Validators.required, futureDateValidator()]
      ],
      status: [this.appointment?.status || 'SCHEDULED', Validators.required],
      notes: [this.appointment?.notes || '', [Validators.maxLength(255)]]
    });

    this.patientService.getAll().subscribe({
      next: (data) => this.patients = data,
      error: (err) => console.error('Error fetching patients', err)
    });
  }

  onSubmit(): void {
    if (this.appointmentForm.invalid) {
      alert('Por favor, complete todos los campos obligatorios.');
      return;
    }

    this.isSubmitting = true;
    const formValue = this.appointmentForm.getRawValue(); // Get disabled fields too
    const newAppointment = {
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
        alert('Ocurrió un error al guardar la cita.');
      }
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
