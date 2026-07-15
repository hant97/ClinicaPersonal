import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { ClinicalSession } from '../../../core/models/clinical-session.model';

@Component({
  selector: 'app-clinical-session-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './clinical-session-form.component.html',
  styleUrls: ['./clinical-session-form.component.css']
})
export class ClinicalSessionFormComponent implements OnInit {
  @Input() patientId!: number;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  sessionForm!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private sessionService: ClinicalSessionService
  ) {}

  ngOnInit(): void {
    const now = new Date();
    const tzOffset = now.getTimezoneOffset() * 60000;
    const localISO = new Date(now.getTime() - tzOffset).toISOString();
    const dateStr = localISO.split('T')[0];
    const timeStr = localISO.split('T')[1].substring(0, 5);

    this.sessionForm = this.fb.group({
      sessionDate: [dateStr, Validators.required],
      startTime: [timeStr, Validators.required],
      endTime: ['', Validators.required],
      sessionType: ['INDIVIDUAL', Validators.required],
      modality: ['PRESENCIAL', Validators.required],
      status: ['COMPLETADA', Validators.required],
      subjective: [''],
      objective: [''],
      analysis: [''],
      plan: [''],
      isConfidential: [false]
    }, { validators: this.soapValidator });
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

    this.sessionService.createSession(sessionData).subscribe({
      next: () => this.saved.emit(),
      error: (err) => console.error('Error saving session', err)
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
