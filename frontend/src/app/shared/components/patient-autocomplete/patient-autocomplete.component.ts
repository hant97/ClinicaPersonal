import { Component, ElementRef, forwardRef, HostListener, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { debounceTime, distinctUntilChanged, Subject, switchMap, takeUntil, tap, of, map } from 'rxjs';

@Component({
  selector: 'app-patient-autocomplete',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-autocomplete.component.html',
  styleUrls: ['./patient-autocomplete.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PatientAutocompleteComponent),
      multi: true
    }
  ]
})
export class PatientAutocompleteComponent implements OnInit, OnDestroy, ControlValueAccessor {
  @Input() disabled = false;
  
  searchControl = new FormControl({ value: '', disabled: this.disabled });
  patients: Patient[] = [];
  showDropdown = false;
  isLoading = false;
  selectedPatientId: number | null = null;
  
  private destroy$ = new Subject<void>();
  
  // ControlValueAccessor functions
  onChange: any = () => {};
  onTouched: any = () => {};

  constructor(
    private patientService: PatientService,
    private elementRef: ElementRef
  ) {}

  ngOnInit(): void {
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
        // If the user selects a patient, the input value is the patient's name,
        // we don't want to search again if they just selected.
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
    if (this.patients.length > 0) {
      this.showDropdown = true;
    }
  }
  
  onInputClear(): void {
      this.selectedPatientId = null;
      this.searchControl.setValue('');
      this.onChange(null);
  }

  // ControlValueAccessor implementation
  writeValue(obj: any): void {
    this.selectedPatientId = obj;
    if (obj) {
      // If we only have the ID, we need to fetch the patient to show the name.
      // In a real app we might pass the patient object, or we do a quick fetch.
      this.patientService.getById(obj).subscribe(patient => {
         if(patient) {
             this.searchControl.setValue(`${patient.firstName} ${patient.lastName}`, { emitEvent: false });
         }
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
