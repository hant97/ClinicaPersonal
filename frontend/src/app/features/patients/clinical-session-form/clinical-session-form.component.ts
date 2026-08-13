import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { ToastService } from '../../../shared/services/toast/toast.service';

@Component({
  selector: 'app-clinical-session-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './clinical-session-form.component.html',
})
export class ClinicalSessionFormComponent implements OnInit {
  @Input() patientId!: number;
  @Input() session?: ClinicalSession;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  sessionForm!: FormGroup;
  appointmentModalities: CatalogItem[] = [];

  constructor(
    private fb: FormBuilder,
    private sessionService: ClinicalSessionService,
    private catalogService: CatalogService,
    private toastService: ToastService,
    private specialtyService: SpecialtyService
  ) {}

  get isPsychology(): boolean {
    return this.specialtyService.isPsychology();
  }

  get isDermatology(): boolean {
    return this.specialtyService.isDermatology();
  }

  ngOnInit(): void {
    this.loadCatalogs();

    let dateStr = '';
    let startTimeStr = '';
    let endTimeStr = '';

    if (this.session) {
      dateStr = this.session.sessionDate;
      if (dateStr.includes('T')) {
        dateStr = dateStr.split('T')[0];
      }
      startTimeStr = this.session.startTime.substring(0, 5);
      endTimeStr = this.session.endTime ? this.session.endTime.substring(0, 5) : '';
    } else {
      const now = new Date();
      const tzOffset = now.getTimezoneOffset() * 60000;
      const localISO = new Date(now.getTime() - tzOffset).toISOString();
      dateStr = localISO.split('T')[0];
      startTimeStr = '';
    }

    let sType = this.session?.sessionType || 'INDIVIDUAL';
    if (sType === 'Terapia Individual') {
      sType = 'INDIVIDUAL';
    }

    this.sessionForm = this.fb.group({
      sessionDate: [dateStr, Validators.required],
      startTime: [startTimeStr, Validators.required],
      endTime: [endTimeStr, Validators.required],
      sessionType: [sType, Validators.required],
      modality: [this.session?.modality || 'PRESENCIAL', Validators.required],
      status: [this.session?.status || 'COMPLETADA', Validators.required],
      subjective: [this.session?.subjective || ''],
      objective: [this.session?.objective || ''],
      analysis: [this.session?.analysis || ''],
      plan: [this.session?.plan || ''],
      skinExamFindings: [this.session?.skinExamFindings || ''],
      dermatologicalDiagnosis: [this.session?.dermatologicalDiagnosis || ''],
      proceduresPerformed: [this.session?.proceduresPerformed || ''],
      prescriptions: [this.session?.prescriptions || ''],
      isConfidential: [this.session?.isConfidential || false]
    }, { validators: this.isPsychology ? this.soapValidator : this.dermValidator });
  }

  loadCatalogs(): void {
    const code = this.isDermatology ? 'DERM_MODALITY' : 'APPOINTMENT_MODALITY';
    this.catalogService.getActiveItemsByCatalogCode(code).subscribe({
      next: (items) => {
        this.appointmentModalities = items;
      },
      error: () => {
        this.toastService.show('Error al cargar modalidades', 'error');
      }
    });
  }

  // Validador custom: Al menos un campo SOAP debe estar lleno
  soapValidator(group: FormGroup): { [key: string]: boolean } | null {
    const s = group.get('subjective')?.value?.trim();
    const o = group.get('objective')?.value?.trim();
    const a = group.get('analysis')?.value?.trim();
    const p = group.get('plan')?.value?.trim();

    if (!s && !o && !a && !p) {
      return { 'soapRequired': true };
    }
    return null;
  }

  dermValidator = (group: FormGroup): { [key: string]: boolean } | null => {
    const findings = group.get('skinExamFindings')?.value?.trim();
    const diag = group.get('dermatologicalDiagnosis')?.value?.trim();

    if (!findings && !diag) {
      return { 'dermRequired': true };
    }
    return null;
  }

  onSubmit(): void {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }

    const sessionData: ClinicalSession = {
      ...this.sessionForm.value,
      patientId: this.patientId
    };

    if (this.session && this.session.id) {
      this.sessionService.updateSession(this.session.id, sessionData).subscribe({
        next: () => {
          this.toastService.show('Sesión actualizada exitosamente', 'success');
          this.saved.emit();
        },
        error: (err) => {
          console.error('Error updating session', err);
          this.toastService.show('Error al actualizar la sesión', 'error');
        }
      });
    } else {
      this.sessionService.createSession(sessionData).subscribe({
        next: () => {
          this.toastService.show('Sesión guardada exitosamente', 'success');
          this.saved.emit();
        },
        error: (err) => {
          console.error('Error saving session', err);
          this.toastService.show('Error al guardar la sesión', 'error');
        }
      });
    }
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
