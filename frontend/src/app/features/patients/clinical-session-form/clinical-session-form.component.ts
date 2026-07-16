import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { ToastService } from '../../../shared/services/toast/toast.service';

@Component({
  selector: 'app-clinical-session-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './clinical-session-form.component.html',
  styleUrls: ['./clinical-session-form.component.css']
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
    private toastService: ToastService
  ) {}

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
      startTimeStr = localISO.split('T')[1].substring(0, 5);
    }

    this.sessionForm = this.fb.group({
      sessionDate: [dateStr, Validators.required],
      startTime: [startTimeStr, Validators.required],
      endTime: [endTimeStr, Validators.required],
      sessionType: [this.session?.sessionType || 'INDIVIDUAL', Validators.required],
      modality: [this.session?.modality || 'PRESENCIAL', Validators.required],
      status: [this.session?.status || 'COMPLETADA', Validators.required],
      subjective: [this.session?.subjective || ''],
      objective: [this.session?.objective || ''],
      analysis: [this.session?.analysis || ''],
      plan: [this.session?.plan || ''],
      isConfidential: [this.session?.isConfidential || false]
    }, { validators: this.soapValidator });
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
