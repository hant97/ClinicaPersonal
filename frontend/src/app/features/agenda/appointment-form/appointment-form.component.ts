import { Component, EventEmitter, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppointmentService } from '../../../core/services/appointment.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';

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
      patientId: ['', Validators.required],
      appointmentDate: [new Date().toISOString().substring(0, 16), Validators.required],
      status: ['SCHEDULED', Validators.required],
      notes: ['']
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
    const formValue = this.appointmentForm.value;
    const newAppointment = {
      ...formValue,
      patientId: Number(formValue.patientId)
    };
    
    this.appointmentService.create(newAppointment).subscribe({
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
