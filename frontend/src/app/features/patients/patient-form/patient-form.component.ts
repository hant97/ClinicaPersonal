import { Component, EventEmitter, Output, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { PatientService } from '../../../core/services/patient/patient.service';
import { ToastService } from '../../../shared/services/toast/toast.service';

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-form.component.html',
  styleUrls: ['./patient-form.component.css']
})
export class PatientFormComponent implements OnInit {
  @Input() patientId: number | null = null;
  @Output() closeModal = new EventEmitter<boolean>();
  
  patientForm: FormGroup;
  isSaving = false;

  constructor(
    private fb: FormBuilder,
    private patientService: PatientService,
    private toastService: ToastService
  ) {
    this.patientForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      identificationDocument: ['', Validators.required],
      contactNumber: [''],
      email: [''],
      dateOfBirth: [''],
      occupation: [''],
      maritalStatus: [''],
      emergencyContact: ['']
    });
  }

  ngOnInit(): void {
    if (this.patientId) {
      this.patientService.getById(this.patientId).subscribe({
        next: (patient) => {
          this.patientForm.patchValue(patient);
        },
        error: () => {
          this.toastService.show('Error al cargar datos del paciente', 'error');
        }
      });
    }
  }

  onSubmit(): void {
    if (this.patientForm.valid) {
      this.isSaving = true;
      const operation = this.patientId 
        ? this.patientService.update(this.patientId, this.patientForm.value)
        : this.patientService.create(this.patientForm.value);

      operation.subscribe({
        next: () => {
          const msg = this.patientId ? 'Paciente actualizado exitosamente' : 'Paciente guardado exitosamente';
          this.toastService.show(msg, 'success');
          this.closeModal.emit(true);
        },
        error: () => {
          const msg = this.patientId ? 'Error al actualizar el paciente' : 'Error al guardar el paciente';
          this.toastService.show(msg, 'error');
          this.isSaving = false;
        }
      });
    }
  }

  cancel(): void {
    this.closeModal.emit(false);
  }
}
