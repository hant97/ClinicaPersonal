import { Component, Input, OnInit } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { DermatologicalHistoryService } from '../../../core/services/dermatological-history.service';
import { DermatologicalHistory } from '../../../core/models/dermatological-history.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule } from 'lucide-angular';
import {
  FileText } from '../../../shared/icons/lucide-icons';

@Component({
  selector: 'app-dermatological-history-section',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './dermatological-history-section.component.html',
})
export class DermatologicalHistorySectionComponent implements OnInit {
  readonly FileText = FileText;

  @Input() patientId!: number;

  history: DermatologicalHistory | null = null;
  form!: FormGroup;
  editing = false;
  loading = true;
  isSaving = false;

  constructor(
    private fb: FormBuilder,
    private service: DermatologicalHistoryService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.service.get(this.patientId).subscribe({
      next: (data) => {
        this.history = data.id ? data : null;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar antecedentes dermatológicos', 'error');
      }
    });
  }

  startEdit(): void {
    this.form = this.fb.group({
      skinType: [this.history?.skinType || ''],
      sunExposureHabits: [this.history?.sunExposureHabits || ''],
      personalSkinHistory: [this.history?.personalSkinHistory || ''],
      familySkinHistory: [this.history?.familySkinHistory || ''],
      chronicConditions: [this.history?.chronicConditions || ''],
      examFindings: [this.history?.examFindings || ''],
      notes: [this.history?.notes || '']
    });
    this.editing = true;
  }

  cancel(): void {
    this.editing = false;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    const data: DermatologicalHistory = { ...this.form.value, patientId: this.patientId };
    this.service.upsert(this.patientId, data).subscribe({
      next: (saved) => {
        this.history = saved;
        this.editing = false;
        this.isSaving = false;
        this.toast.show('Antecedentes dermatológicos guardados', 'success');
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar antecedentes', 'error');
      }
    });
  }

  hasContent(): boolean {
    return !!(
      this.history?.skinType ||
      this.history?.sunExposureHabits ||
      this.history?.personalSkinHistory ||
      this.history?.familySkinHistory ||
      this.history?.chronicConditions ||
      this.history?.examFindings ||
      this.history?.notes
    );
  }
}
