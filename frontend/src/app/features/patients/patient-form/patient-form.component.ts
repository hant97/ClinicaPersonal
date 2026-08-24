import { Component, EventEmitter, Output, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PatientService } from '../../../core/services/patient/patient.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { CatalogItem } from '../../../core/models/catalog.model';
import {
  LucideAngularModule,
  User,
  Phone,
  Mail,
  MapPin,
  FileText,
  ArrowLeft,
  Save,
  CheckCircle2,
  ShieldAlert,
  AlertCircle,
  Info,
  Calendar,
  Sparkles,
  Building2,
  Briefcase,
  Heart,
  UserPlus
} from 'lucide-angular';

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './patient-form.component.html',
})
export class PatientFormComponent implements OnInit {
  // Lucide Icons
  readonly User = User;
  readonly Phone = Phone;
  readonly Mail = Mail;
  readonly MapPin = MapPin;
  readonly FileText = FileText;
  readonly ArrowLeft = ArrowLeft;
  readonly Save = Save;
  readonly CheckCircle2 = CheckCircle2;
  readonly ShieldAlert = ShieldAlert;
  readonly AlertCircle = AlertCircle;
  readonly Info = Info;
  readonly Calendar = Calendar;
  readonly Sparkles = Sparkles;
  readonly Building2 = Building2;
  readonly Briefcase = Briefcase;
  readonly Heart = Heart;
  readonly UserPlus = UserPlus;

  @Input() patientId: number | string | null = null;
  @Output() closeModal = new EventEmitter<boolean>();

  patientForm: FormGroup;
  isSaving = false;
  isLoading = false;
  isDermatology = false;
  isEditMode = false;
  patientIdentifier: string | null = null;
  internalPatientId: number | null = null;

  documentTypes: CatalogItem[] = [];
  genders: CatalogItem[] = [];
  maritalStatuses: CatalogItem[] = [];
  esMenorEdad = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private patientService: PatientService,
    private catalogService: CatalogService,
    private toastService: ToastService,
    private specialtyService: SpecialtyService
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
      guardianContact: ['', [Validators.pattern(/^[0-9+\-\s()]+$/), Validators.minLength(7), Validators.maxLength(15)]],
      active: [true]
    });
  }

  ngOnInit(): void {
    this.isDermatology = this.specialtyService.isDermatology();
    this.loadCatalogs();
    this.setupDocumentValidation();
    this.setupAgeValidation();

    const routeIdentifier = this.route.snapshot.paramMap.get('identifier') || this.route.snapshot.paramMap.get('id');
    const targetIdentifier = routeIdentifier || (this.patientId ? String(this.patientId) : null);

    if (targetIdentifier && targetIdentifier !== 'new') {
      this.isEditMode = true;
      this.patientIdentifier = targetIdentifier;
      this.loadPatientData(this.patientIdentifier);
    }
  }

  loadPatientData(identifier: string): void {
    this.isLoading = true;
    this.patientService.getById(identifier).subscribe({
      next: (patient) => {
        this.internalPatientId = patient.id ?? null;
        this.patientIdentifier = patient.uuid || String(patient.id || identifier);
        this.patientForm.patchValue({ ...patient, active: patient.active !== false }, { emitEvent: false });
        this.checkAge(patient.dateOfBirth);
        this.isLoading = false;
      },
      error: () => {
        this.toastService.show('Error al cargar datos del paciente', 'error');
        this.isLoading = false;
      }
    });
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
      if (type === 'DNI') {
        docControl?.setValidators([Validators.required, Validators.minLength(8), Validators.maxLength(8), Validators.pattern(/^[0-9]+$/)]);
      } else {
        docControl?.setValidators([Validators.required, Validators.minLength(4), Validators.maxLength(20), Validators.pattern(/^[a-zA-Z0-9]+$/)]);
      }
      docControl?.updateValueAndValidity();
    });
  }

  onSubmit(): void {
    if (this.patientForm.invalid) {
      this.patientForm.markAllAsTouched();
      this.toastService.show('Por favor, completa correctamente todos los campos obligatorios.', 'error');
      return;
    }

    this.isSaving = true;

    if (this.isEditMode && (this.internalPatientId || this.patientIdentifier)) {
      const updateId = this.internalPatientId ?? Number(this.patientIdentifier);
      this.patientService.update(updateId, this.patientForm.value).subscribe({
        next: (updatedPatient) => {
          this.toastService.show('Paciente actualizado exitosamente', 'success');
          this.isSaving = false;
          this.closeModal.emit(true);
          const target = updatedPatient.uuid || this.patientIdentifier || String(updateId);
          this.router.navigate(['/patients', target]);
        },
        error: () => {
          this.toastService.show('Error al actualizar el paciente', 'error');
          this.isSaving = false;
        }
      });
    } else {
      this.patientService.create(this.patientForm.value).subscribe({
        next: (createdPatient) => {
          this.toastService.show('Paciente registrado exitosamente', 'success');
          this.isSaving = false;
          this.closeModal.emit(true);
          const target = createdPatient.uuid || createdPatient.id;
          if (target) {
            this.router.navigate(['/patients', target]);
          } else {
            this.router.navigate(['/patients']);
          }
        },
        error: () => {
          this.toastService.show('Error al guardar el paciente', 'error');
          this.isSaving = false;
        }
      });
    }
  }

  cancel(): void {
    this.closeModal.emit(false);
    if (this.isEditMode && this.patientIdentifier) {
      this.router.navigate(['/patients', this.patientIdentifier]);
    } else {
      this.router.navigate(['/patients']);
    }
  }
}
