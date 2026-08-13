import { Component, ElementRef, forwardRef, HostListener, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, FormBuilder, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule, Validators } from '@angular/forms';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { CatalogService } from '../../../core/services/catalog.service';
import { CatalogItem } from '../../../core/models/catalog.model';
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
  activeIndex = -1;

  showQuickAddModal = false;
  quickAddForm!: FormGroup;
  isSavingQuickPatient = false;
  genders: CatalogItem[] = [];
  
  private destroy$ = new Subject<void>();
  
  // ControlValueAccessor functions
  onChange: any = () => {};
  onTouched: any = () => {};

  constructor(
    private patientService: PatientService,
    private toastService: ToastService,
    private fb: FormBuilder,
    private elementRef: ElementRef,
    private catalogService: CatalogService
  ) {}

  ngOnInit(): void {
    this.quickAddForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.maxLength(50)]],
      identificationDocument: ['', [Validators.required, Validators.maxLength(20)]],
      contactNumber: ['', [Validators.maxLength(20)]],
      email: ['', [Validators.email]],
      gender: ['', [Validators.required]]
    });

    this.catalogService.getActiveItemsByCatalogCode('GENDER').subscribe({
      next: (items) => this.genders = items,
      error: () => this.genders = []
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
      this.activeIndex = -1;
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
    this.activeIndex = -1;
    this.searchControl.setValue(`${patient.firstName} ${patient.lastName}`, { emitEvent: false });
    this.showDropdown = false;
    this.onChange(this.selectedPatientId);
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.showDropdown = false;
      this.activeIndex = -1;
      return;
    }
    if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp' && event.key !== 'Enter') {
      return;
    }
    if (!this.showDropdown || this.patients.length === 0) {
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        this.showDropdown = this.patients.length > 0;
      }
      return;
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeIndex = (this.activeIndex + 1) % this.patients.length;
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeIndex = (this.activeIndex - 1 + this.patients.length) % this.patients.length;
    } else if (event.key === 'Enter') {
      if (this.activeIndex >= 0 && this.activeIndex < this.patients.length) {
        event.preventDefault();
        this.selectPatient(this.patients[this.activeIndex]);
      }
    }
  }

  onFocus(): void {
    if (this.patients.length > 0 || (this.searchControl.value && this.searchControl.value.length >= 2)) {
      this.showDropdown = true;
    }
  }
  
  onInputClear(): void {
    this.selectedPatientId = null;
    this.activeIndex = -1;
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
      email: '',
      gender: ''
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
      email: formVal.email?.trim() || undefined,
      gender: formVal.gender
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
