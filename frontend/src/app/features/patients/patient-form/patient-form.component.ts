import { Component, EventEmitter, Output, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { PatientService } from '../../../core/services/patient/patient.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { CatalogItem } from '../../../core/models/catalog.model';

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

  documentTypes: CatalogItem[] = [];
  genders: CatalogItem[] = [];
  maritalStatuses: CatalogItem[] = [];

  constructor(
    private fb: FormBuilder,
    private patientService: PatientService,
    private catalogService: CatalogService,
    private toastService: ToastService
  ) {
    this.patientForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50), Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)]],
      lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50), Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)]],
      documentType: ['', [Validators.required]],
      identificationDocument: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(8), Validators.pattern(/^[0-9]+$/)]],
      contactNumber: ['', [Validators.pattern(/^[0-9+\-\s()]+$/), Validators.minLength(7), Validators.maxLength(15)]],
      email: ['', [Validators.email]],
      dateOfBirth: [''],
      occupation: ['', [Validators.maxLength(100)]],
      maritalStatus: [''],
      emergencyContact: ['', [Validators.maxLength(100)]],
      reasonForConsultation: ['', [Validators.maxLength(500)]],
      gender: ['', [Validators.required]],
      address: ['', [Validators.maxLength(255)]],
      hasLegalGuardian: [false],
      guardianName: ['', [Validators.maxLength(100), Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)]],
      guardianContact: ['', [Validators.pattern(/^[0-9+\-\s()]+$/), Validators.minLength(7), Validators.maxLength(15)]]
    });
  }

  esMenorEdad = false;

  ngOnInit(): void {
    this.loadCatalogs();
    this.setupDocumentValidation();
    this.setupAgeValidation();

    if (this.patientId) {
      this.patientService.getById(this.patientId).subscribe({
        next: (patient) => {
          this.patientForm.patchValue(patient);
          this.checkAge(patient.dateOfBirth);
        },
        error: () => {
          this.toastService.show('Error al cargar datos del paciente', 'error');
        }
      });
    }
  }

  setupAgeValidation(): void {
    this.patientForm.get('dateOfBirth')?.valueChanges.subscribe(date => {
      this.checkAge(date);
    });
    this.patientForm.get('hasLegalGuardian')?.valueChanges.subscribe(() => {
      this.updateGuardianValidators();
    });
  }

  checkAge(dateOfBirth: string | undefined): void {
    if (dateOfBirth) {
      const birthDate = new Date(dateOfBirth);
      const today = new Date();
      let age = today.getFullYear() - birthDate.getFullYear();
      const m = today.getMonth() - birthDate.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
        age--;
      }
      this.esMenorEdad = age < 18;
    } else {
      this.esMenorEdad = false;
    }
    this.updateGuardianValidators();
  }

  updateGuardianValidators(): void {
    const hasLegalGuardian = this.patientForm.get('hasLegalGuardian')?.value;
    const gName = this.patientForm.get('guardianName');
    const gContact = this.patientForm.get('guardianContact');

    if (!this.esMenorEdad && !hasLegalGuardian) {
      gName?.setValue('');
      gContact?.setValue('');
    }
  }

  loadCatalogs(): void {
    forkJoin({
      docTypes: this.catalogService.getActiveItemsByCatalogCode('DOCUMENT_TYPE'),
      genders: this.catalogService.getActiveItemsByCatalogCode('GENDER'),
      maritalStatuses: this.catalogService.getActiveItemsByCatalogCode('MARITAL_STATUS')
    }).subscribe({
      next: (results) => {
        this.documentTypes = results.docTypes;
        this.genders = results.genders;
        this.maritalStatuses = results.maritalStatuses;
      },
      error: () => {
        this.toastService.show('Error al cargar los catálogos', 'error');
      }
    });
  }

  setupDocumentValidation(): void {
    this.patientForm.get('documentType')?.valueChanges.subscribe(type => {
      const docControl = this.patientForm.get('identificationDocument');
      docControl?.setValue('');
      
      if (type === 'DNI') {
        docControl?.setValidators([Validators.required, Validators.minLength(8), Validators.maxLength(8), Validators.pattern(/^[0-9]+$/)]);
      } else {
        docControl?.setValidators([Validators.required, Validators.minLength(4), Validators.maxLength(20), Validators.pattern(/^[a-zA-Z0-9]+$/)]);
      }
      docControl?.updateValueAndValidity();
    });
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
