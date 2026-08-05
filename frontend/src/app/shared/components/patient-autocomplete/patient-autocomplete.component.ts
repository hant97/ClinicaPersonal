import { Component, ElementRef, forwardRef, HostListener, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, FormBuilder, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule, Validators } from '@angular/forms';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule, UserPlus, X, User, Phone, Mail, FileText } from 'lucide-angular';
import { debounceTime, distinctUntilChanged, Subject, switchMap, takeUntil, tap, of, map } from 'rxjs';

@Component({
  selector: 'app-patient-autocomplete',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './patient-autocomplete.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PatientAutocompleteComponent),
      multi: true
    }
  ]
})
export class PatientAutocompleteComponent implements OnInit, OnDestroy, ControlValueAccessor {
  readonly UserPlus = UserPlus;
  readonly X = X;
  readonly User = User;
  readonly Phone = Phone;
  readonly Mail = Mail;
  readonly FileText = FileText;

  @Input() disabled = false;
  @Input() enableQuickAdd = true;
  
  searchControl = new FormControl({ value: '', disabled: this.disabled });
  patients: Patient[] = [];
  showDropdown = false;
  isLoading = false;
  selectedPatientId: number | null = null;

  showQuickAddModal = false;
  quickAddForm!: FormGroup;
  isSavingQuickPatient = false;
  
  private destroy$ = new Subject<void>();
  
  // ControlValueAccessor functions
  onChange: any = () => {};
  onTouched: any = () => {};

  constructor(
    private patientService: PatientService,
    private toastService: ToastService,
    private fb: FormBuilder,
    private elementRef: ElementRef
  ) {}

  ngOnInit(): void {
    this.quickAddForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.maxLength(50)]],
      identificationDocument: ['', [Validators.required, Validators.maxLength(20)]],
      contactNumber: ['', [Validators.maxLength(20)]],
      email: ['', [Validators.email]]
    });

    this.searchControl.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => this.isLoading = true),
      switchMap(query => {
        if (!query || query.length < 2) {
          this.patients = [];
          return of([]);
        }
        if (this.selectedPatientId !== null) {
          const currentSelected = this.patients.find(p => p.id === this.selectedPatientId);
          if (currentSelected && query === `${currentSelected.firstName} ${currentSelected.lastName}`) {
            return of([]);
          }
        }
        return this.patientService.search(query).pipe(
          map(page => page.content)
        );
      }),
      tap(() => this.isLoading = false)
    ).subscribe(results => {
      if (results.length > 0 || (this.searchControl.value && this.searchControl.value.length >= 2)) {
        this.patients = results;
        this.showDropdown = true;
      } else {
        this.patients = [];
        this.showDropdown = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Handle click outside to close dropdown
  @HostListener('document:click', ['$event'])
  onClickOutside(event: Event) {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.showDropdown = false;
      this.onTouched();
    }
  }

  selectPatient(patient: Patient): void {
    this.selectedPatientId = patient.id!;
    this.searchControl.setValue(`${patient.firstName} ${patient.lastName}`, { emitEvent: false });
    this.showDropdown = false;
    this.onChange(this.selectedPatientId);
  }

  onFocus(): void {
    if (this.patients.length > 0 || (this.searchControl.value && this.searchControl.value.length >= 2)) {
      this.showDropdown = true;
    }
  }
  
  onInputClear(): void {
    this.selectedPatientId = null;
    this.searchControl.setValue('');
    this.onChange(null);
  }

  openQuickAdd(): void {
    this.showDropdown = false;
    const val = this.searchControl.value?.trim() || '';
    let prefillDoc = '';
    let prefillFirst = '';
    if (/^\d+$/.test(val)) {
      prefillDoc = val;
    } else if (val) {
      const parts = val.split(' ');
      prefillFirst = parts[0] || '';
    }

    this.quickAddForm.reset({
      firstName: prefillFirst,
      lastName: '',
      identificationDocument: prefillDoc,
      contactNumber: '',
      email: ''
    });
    this.showQuickAddModal = true;
  }

  closeQuickAdd(): void {
    this.showQuickAddModal = false;
    this.quickAddForm.reset();
  }

  saveQuickPatient(): void {
    if (this.quickAddForm.invalid) {
      this.quickAddForm.markAllAsTouched();
      this.toastService.show('Por favor complete los campos obligatorios del paciente.', 'error');
      return;
    }

    this.isSavingQuickPatient = true;
    const formVal = this.quickAddForm.value;
    const newPatient: Patient = {
      firstName: formVal.firstName.trim(),
      lastName: formVal.lastName.trim(),
      identificationDocument: formVal.identificationDocument.trim(),
      contactNumber: formVal.contactNumber?.trim() || undefined,
      email: formVal.email?.trim() || undefined
    };

    this.patientService.create(newPatient).subscribe({
      next: (created) => {
        this.isSavingQuickPatient = false;
        this.toastService.show('Paciente registrado y seleccionado exitosamente', 'success');
        this.closeQuickAdd();
        this.selectPatient(created);
      },
      error: (err) => {
        this.isSavingQuickPatient = false;
        console.error('Error creating patient quickly', err);
        this.toastService.show(err.error?.message || 'Error al registrar el paciente (verifique si el documento ya existe).', 'error');
      }
    });
  }

  // ControlValueAccessor implementation
  writeValue(obj: any): void {
    this.selectedPatientId = obj;
    if (obj) {
      this.patientService.getById(obj).subscribe({
        next: (patient) => {
          if (patient) {
            this.searchControl.setValue(`${patient.firstName} ${patient.lastName}`, { emitEvent: false });
          }
        },
        error: () => {}
      });
    } else {
      this.searchControl.setValue('', { emitEvent: false });
    }
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.searchControl.disable({ emitEvent: false });
    } else {
      this.searchControl.enable({ emitEvent: false });
    }
  }
}
