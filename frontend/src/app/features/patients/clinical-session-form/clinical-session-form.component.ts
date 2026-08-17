import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import {
  LucideAngularModule,
  Clock,
  Calendar,
  FileText,
  Lock,
  Sparkles,
  Check,
  X,
  AlertCircle
} from 'lucide-angular';

@Component({
  selector: 'app-clinical-session-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './clinical-session-form.component.html',
})
export class ClinicalSessionFormComponent implements OnInit {
  readonly Clock = Clock;
  readonly Calendar = Calendar;
  readonly FileText = FileText;
  readonly Lock = Lock;
  readonly Sparkles = Sparkles;
  readonly Check = Check;
  readonly X = X;
  readonly AlertCircle = AlertCircle;

  @Input() patientId!: number;
  @Input() session?: ClinicalSession;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  sessionForm!: FormGroup;
  appointmentModalities: CatalogItem[] = [];
  draftSaved = false;

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

  private get draftKey(): string {
    return `flowgrid_draft_session_${this.patientId}`;
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
      const curH = String(now.getHours()).padStart(2, '0');
      const curM = String(now.getMinutes()).padStart(2, '0');
      startTimeStr = `${curH}:${curM}`;
      
      const endMins = now.getHours() * 60 + now.getMinutes() + 45;
      const endH = String(Math.floor(endMins / 60) % 24).padStart(2, '0');
      const endM = String(endMins % 60).padStart(2, '0');
      endTimeStr = `${endH}:${endM}`;
    }

    let sType = this.session?.sessionType || 'INDIVIDUAL';
    if (sType === 'Terapia Individual') {
      sType = 'INDIVIDUAL';
    }

    // Check for draft if creating new
    let savedDraft: any = null;
    if (!this.session) {
      try {
        const raw = localStorage.getItem(this.draftKey);
        if (raw) {
          savedDraft = JSON.parse(raw);
        }
      } catch (e) {}
    }

    this.sessionForm = this.fb.group({
      sessionDate: [this.session?.sessionDate || savedDraft?.sessionDate || dateStr, Validators.required],
      startTime: [this.session?.startTime || savedDraft?.startTime || startTimeStr, Validators.required],
      endTime: [this.session?.endTime || savedDraft?.endTime || endTimeStr, Validators.required],
      sessionType: [this.session?.sessionType || savedDraft?.sessionType || sType, Validators.required],
      modality: [this.session?.modality || savedDraft?.modality || 'PRESENCIAL', Validators.required],
      status: [this.session?.status || savedDraft?.status || 'COMPLETADA', Validators.required],
      subjective: [this.session?.subjective || savedDraft?.subjective || ''],
      objective: [this.session?.objective || savedDraft?.objective || ''],
      analysis: [this.session?.analysis || savedDraft?.analysis || ''],
      plan: [this.session?.plan || savedDraft?.plan || ''],
      isConfidential: [this.session?.isConfidential || savedDraft?.isConfidential || false]
    }, { validators: this.soapValidator });

    // Auto-save draft on value changes
    if (!this.session) {
      this.sessionForm.valueChanges.subscribe(val => {
        try {
          localStorage.setItem(this.draftKey, JSON.stringify(val));
          this.draftSaved = true;
        } catch (e) {}
      });
    }
  }

  setDuration(minutes: number): void {
    const start = this.sessionForm.get('startTime')?.value;
    if (start && start.includes(':')) {
      const [h, m] = start.split(':').map(Number);
      const totalMins = h * 60 + m + minutes;
      const endH = String(Math.floor(totalMins / 60) % 24).padStart(2, '0');
      const endM = String(totalMins % 60).padStart(2, '0');
      this.sessionForm.patchValue({ endTime: `${endH}:${endM}` });
    }
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

  // Quick insertion helpers for SOAP blocks
  insertTemplate(field: 'subjective' | 'objective' | 'analysis' | 'plan', text: string): void {
    const control = this.sessionForm.get(field);
    if (control) {
      const current = control.value ? `${control.value}\n${text}` : text;
      control.setValue(current);
    }
  }

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
          this.clearDraft();
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
          this.clearDraft();
          this.saved.emit();
        },
        error: (err) => {
          console.error('Error saving session', err);
          this.toastService.show('Error al guardar la sesión', 'error');
        }
      });
    }
  }

  private clearDraft(): void {
    try {
      localStorage.removeItem(this.draftKey);
    } catch (e) {}
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
